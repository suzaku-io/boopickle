# Internal details

## Efficient coding

BooPickle makes assumptions of what kind of data it needs to encode, to reach high efficiency in typical scenarios. For example an `Int` (which takes
32-bits or 4 bytes) is encoded in 1-5 bytes depending on the value. The most common values (0-127) take only a single byte, whereas larger values
require more bytes. Because very large integers take 5 bytes, if you know your data consists mainly of such values, you could specifically code them
using `raw` format that always takes 32-bits. Similarly `Long`s are also coded in 1-9 bytes depending on the value.

In many situations there is a need to encode a length (of String, Seq, Map, etc.) and the efficient Int coding is used. But because a length/size is always
non-negative, we can use negative integers to indicate other things. BooPickle supports coding multiple instances of the same object reference by using
a reference value. The length value is reused to encode the reference by just flipping it into a negative value.

Note that when using the speed optimized codecs (`EncodeSpeed` and `DecodeSpeed`) some of these size optimizations are not used.

## Automatic pickler generation with macros

Scala features powerful [macros](http://docs.scala-lang.org/overviews/macros/overview.html) to help simplifying many mundane programming tasks. One
such task is writing pickling functions for classes. Consider the simple case class below:

```scala
case class Person(foreName: String, lastName: String, email: String, birthYear: Int)
```

To pickle this, you'd need to write following code:

```scala
implicit object PersonPickler extends Pickler[Person] {
  override def pickle(value: Person)(implicit state: PickleState): Unit = {
    state.pickle(value.foreName)
    state.pickle(value.lastName)
    state.pickle(value.email)
    state.pickle(value.birthYear)
  }
  override def unpickle(implicit state: UnpickleState): Person = {
    Person( 
      state.unpickle[String],
      state.unpickle[String],
      state.unpickle[String],
      state.unpickle[Int]
    )
  }
}
```

This would be very tedious, which is why practically all serialization libraries use either reflection or macros to automate this task. BooPickle
being fully compatible with Scala.js, reflection is not an option, so macros it is. Programming macros in Scala is quite difficult, because it's a very
recent addition to the Scala compiler and the documentation tends to be terse and somewhat cryptic. Also many examples found in the net are already
obsolete or wrong if you use Scala 2.11. Best course of action is to look at existing macro code and try to deduce what's going on. Both
uPickle and Prickle provided good base for BooPickle's macros.

The macro-generated picklers are provided by a separate trait to make sure they are the last resort the compiler turns to.

```scala
trait MaterializePicklerFallback {
  implicit def generatePickler[T]: Pickler[T] = macro PicklerMaterializersImpl.materializePickler[T]
}
```

If no other implicit pickler can be found, the compiler will call the `materializePickler` macro function in the hope of generating a suitable one.

The [macro code](https://github.com/suzaku-io/boopickle/blob/master/boopickle/shared/src/main/scala/boopickle/PicklerMaterializersImpl.scala) starts
by checking that the given type is valid for pickling (a sealed trait or a case class). Next step is building the code for pickling individual fields
of the case class, which is surprisingly simple. Scala macros use a concept called quasiquotes (`q"""code goes here"""`) to easily generate code.

```scala
val accessors = (tpe.decls collect {
  case acc: MethodSymbol if acc.isCaseAccessor => acc
}).toList

val pickleFields = for {
  accessor <- accessors
} yield
  q"""state.pickle(value.${accessor.name})"""
```

Because there might be more than one instance of the case class in the structure we are pickling, additional code is generated to check for that
and to store just a reference instead, if needed. For case objects, nothing(!) needs to be stored as they are identified by their type directly.

```scala
val pickleLogic = if (sym.isModuleClass) 
    q"""()""" 
  else q"""
    state.identityRefFor(value) match {
      case Some(idx) =>
        state.enc.writeInt(-idx)
      case None =>
        state.enc.writeInt(0)
        ..$pickleFields
        state.addIdentityRef(value)
    }
  """
```

Finally an `implicit object` is generated to provide the `Pickler` instance.

```scala
val result = q"""
  implicit object $name extends boopickle.Pickler[$tpe] {
    import boopickle._
    override def pickle(value: $tpe)(implicit state: PickleState): Unit = $pickleLogic
    override def unpickle(implicit state: UnpickleState): $tpe = $unpickleLogic
  }
  $name
"""
```

That's it for generating a pickler for a case class! Unpickle logic generation is pretty much the same, check out the code for details.

BooPickle also supports automatic pickler generation for sealed class hierarchies and that functionality is also implemented by the macro. When the macro
first checks if it's a trait, it will continue under a different code path than for case classes. Goal of the macro is to create a `CompositePickler`
for the given trait so that all implementing classes are included.

First step is to make some sanity checks and then find all the known subclasses. This is why the trait must be *sealed* so that the compiler knows
all subclasses and the macro can generate correct code. Next all found subclasses are mapped to `addConcreteType[$s]` code blocks that are embedded into a
generated `CompositePickler`.

```scala
q"""
  implicit object $name extends boopickle.CompositePickler[$tpe] {
    ..$concreteTypes
  }
  $name
"""
```

## Fast UTF-8 coding in the browser

UTF-8 is pretty much the universal character coding format used in the web. For example JSON data is always coded in UTF-8 and naturally all browsers
are very good and efficient at processing UTF-8 formatted text. But when you actually have to do UTF-8 encoding or decoding in JavaScript the situation
is much worse. You can have strings and arrays of bytes, but regular JavaScript doesn't provide decent methods for converting between these two.

One option is to write your own UTF-8 codec and this is exactly what the Scala.js library provides. Its performance is not so great when compared to
native JSON processing but it gets the job done. 

Luckily there is a JavaScript extension known as [TextEncoder](https://developer.mozilla.org/en-US/docs/Web/API/TextEncoder), which provides native
speed encoding of UTF-8 (and some other formats, too). The `TextEncoder` (and `TextDecoder`) work with typed arrays (`Uint8Array` in this case) that
are a high-performance alternative to basic JS arrays.

Because these interfaces are not available on all browsers, the string codec code must check for their availability and fall back to regular implementation
if they are missing.

```scala
class TextEncoder extends js.Object {
  def encode(str: String): Uint8Array = js.native
}

private lazy val utf8encoder: (String) => Int8Array = {
  val te = new TextEncoder
  // use native TextEncoder
  (str: String) => new Int8Array(te.encode(str))
}

def encodeUTF8(s: String): ByteBuffer = {
  if (js.isUndefined(js.Dynamic.global.TextEncoder)) {
    StandardCharsets.UTF_8.encode(s)
  } else {
    TypedArrayBuffer.wrap(utf8encoder(s))
  }
}
```

