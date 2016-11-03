# BooPickle

[![Join the chat at https://gitter.im/ochrons/boopickle](https://badges.gitter.im/ochrons/boopickle.svg)](https://gitter.im/ochrons/boopickle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/ochrons/boopickle.svg?branch=master)](https://travis-ci.org/ochrons/boopickle)
[![Scala.js](http://www.scala-js.org/assets/badges/scalajs-0.6.8.svg)](http://www.scala-js.org)

BooPickle is the [fastest](http://ochrons.github.io/boopickle-perftest/) and most size efficient serialization (aka pickling) library that works on both Scala
and [Scala.js](http://www.scala-js.org). It encodes into a binary format instead of the more customary JSON. A binary format brings efficiency 
gains in both size and speed, at the cost of legibility of the encoded data. BooPickle borrows heavily from both [uPickle](https://github.com/lihaoyi/upickle-pprint)
and [Prickle](https://github.com/benhutchison/prickle) so special thanks to Li Haoyi and Ben Hutchison for those two great libraries!

## Features

- Supports both Scala and Scala.js (no reflection!)
- Serialization support for all primitives, collections, options, tuples and case classes (including class hierarchies)
- User-definable custom serializers
- Transforming serializers to simplify serializing non-case classes
- Handles [references and deduplication of identical objects](#references)
- Very fast
- Very efficient coding
- Low memory usage, no intermediate structures needed
- Zero dependencies
- Scala 2.11/2.12
- All modern browsers are supported (not IE9 and below, though)

## Getting started

Add following dependency declaration to your Scala project 

```scala
"me.chrons" %% "boopickle" % "1.2.4"
```

On a Scala.js project the dependency looks like this

```scala
"me.chrons" %%% "boopickle" % "1.2.4"
```

To use it in your code, simply import the Default object contents. All examples in this document assume this import is present.

```scala
import boopickle.Default._
```

To serialize (pickle) something, just call `Pickle.intoBytes` with your data. This will produce a binary `ByteBuffer` containing an encoded version
of your data.

```scala
val data = Seq("Hello", "World!")
val buf = Pickle.intoBytes(data)
```

And to deserialize (unpickle) the buffer, call `Unpickle.fromBytes`, specifying the type of your data. BooPickle doesn't encode *any* type information,
so you *must* use the same types when pickling and unpickling.

```scala
val helloWorld = Unpickle[Seq[String]].fromBytes(buf)
```

## Supported types

BooPickle has built-in support for most of the typical Scala types, including

- primitives: `Boolean`, `Byte`, `Short`, `Char`, `Int`, `Long`, `Float`, `Double` and `String`
- common types: `Tuple`s, `Option`, `Either`, `Duration`, `UUID`, `BigInt`, `BigDecimal` and `ByteBuffer`
- collections, both mutable and immutable, including: `Array`, `Vector`, `List`, `Set`s, `Map`s and any `Iterable` with a `CanBuildFrom` implementation
- `case class`es and `case object`s (via a macro)
- `trait`s as a base for a class hierarchy

## Class hierarchies

By default, BooPickle encodes zero type information, which makes it impossible to directly encode a class hierarchy like below and decode it
just by specifying the parent type `Fruit`.

```scala
trait Fruit {
  val weight: Double
  def color: String
}

case class Banana(weight: Double) extends Fruit {
  def color = "yellow"
}

case class Kiwi(weight: Double) extends Fruit {
  def color = "brown"
}

case class Carambola(weight: Double) extends Fruit {
  def color = "yellow"
}
```

As this is such a common situation, BooPickle provides a helper class `CompositePickler` to build a custom pickler for composite types. For the case
above, all you need to do is to define an implicit pickler like this, utilizing the `compositePickler` function from `Default`:

```scala
implicit val fruitPickler = compositePickler[Fruit].
  addConcreteType[Banana].
  addConcreteType[Kiwi].
  addConcreteType[Carambola]
```

Now you can freely pickle any `Fruit` and when unpickling, BooPickle will know what type to decode.

```scala
val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
val bb = Pickle.intoBytes(fruits)
.
.
val u = Unpickle[Seq[Fruit]].fromBytes(bb)
assert(u == fruits)
```

Note that internally `CompositePickler` encodes types using indices, so they must be specified in the same order on both sides!

BooPickle needs to know the type when pickling to deserialize to the correct type, thus this fails

```scala
val b = Banana(1.0)
val bb = Pickle.intoBytes(b)
assert(Unpickle[Banana].fromBytes(bb) == b) // This produces Banana
val bb2 = Pickle.intoBytes(b)
assert(Unpickle[Fruit].fromBytes(bb2) == null) // This produces null
```

Instead when pickling declare the parent type

```scala
val f: Fruit = Banana(1.0)
val bf = Pickle.intoBytes(f)
assert(Unpickle[Fruit].fromBytes(bf) == f) // This produces a Fruit
```

### Recursive composite types

If you have a recursive composite type (a sub type has a reference to the super type), you need to build the `CompositePickler` in two steps,
as shown below.

```scala
sealed trait Tree
case object Leaf extends Tree
case class Node(value: Int, children:Seq[Tree]) extends Tree

object Tree {
  implicit val treePickler = compositePickler[Tree]
  treePickler.addConcreteType[Node].addConcreteType[Leaf.type]
}
```

This is because the compiler must find a pickler for `Tree` when it's building a pickler for `Node`.

### Automatic generation of hierarchy picklers

If your type hierarchy is `sealed` then you can take advantage of the automatic pickler generation feature of BooPickle. A macro automatically generates
the required `CompositePickler` for you, as long as the trait is `sealed`. For example lets change the `Fruit` trait to be sealed, so that compiler
knows all its descendants will be defined in the same file and the macro can find them.

```scala
sealed trait Fruit {
  val weight: Double
  def color: String
}
```

Now you can directly pickle your fruits without manually defining a `CompositePickler`.

```scala
val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
val bb = Pickle.intoBytes(fruits)
.
.
val u = Unpickle[Seq[Fruit]].fromBytes(bb)
assert(u == fruits)
```

Note that for some hierarchies the automatic generation may not work (due to Scala compiler limitations), but you can always fall back to the
manually defined `CompositePickler`.

Also note that due to the way macros generate picklers, each time you need an implicit instance of the pickler, new classes (and `.class` files)
will be generated. And not just for the top level trait, but for all implementing classes as well. If you have a large class hierarchy, this adds up
rather quickly! Below you can see the results of pickling a trait twice in the code.

``` 
 Size   Name
 2,798  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$14$TraitPickler$macro$25$2$CCPickler$macro$26$2$.class
 2,798  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$16$TraitPickler$macro$33$2$CCPickler$macro$34$2$.class
 3,498  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$14$TraitPickler$macro$25$2$CCPickler$macro$27$2$.class
 3,498  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$16$TraitPickler$macro$33$2$CCPickler$macro$35$2$.class
 4,789  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$14$TraitPickler$macro$25$2$.class
 4,789  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$16$TraitPickler$macro$33$2$.class
``` 

If this becomes an issue, you can avoid it by storing implicit picklers in the companion object of the trait. This way the code is generated only once
and used whenever you need a pickler for your `Fruit`.

```scala
object Fruit {
  implicit val pickler: Pickler[Fruit] = generatePickler[Fruit]
}

// must import the companion object, otherwise the implicit macro has higher precedence and will generate another pickler!
import Fruit._
val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
val bb = Pickle.intoBytes(fruits)
```

You can prevent the implicit use of the pickler generator macro by importing `boopickle.DefaultBasic._` instead of 
`boopickle.Default._` as this will leave the implicit macro code out. Then you can provide specific implicit picklers for your 
case classes or class hierarchies.

```scala
import boopickle.DefaultBasic._
object Fruit {
  // use macro explicitly to generate the pickler
  implicit val pickler: Pickler[Fruit] = PicklerGenerator.generatePickler[Fruit]
}
```

In this case you don't need to `import Fruit._` because there is no implicit macro to compete with your pickler in the companion object.

Note that when not using implicit macro picklers, you must pay special attention to the creation order of picklers in more complex situations like below.

```scala
import boopickle.DefaultBasic._
sealed trait MyTrait

case class TT1(i: Int) extends MyTrait

case class TT2(s: String, next: MyTrait) extends MyTrait

class TT3(val i: Int, val s: String) extends MyTrait

object MyTrait {
  // picklers must be created in correct order, because TT2 depends on MyTrait
  implicit val pickler = compositePickler[MyTrait]
  // use macro explicitly to generate picklers for TT1 and TT2
  implicit val pickler1 = PicklerGenerator.generatePickler[TT1]
  implicit val pickler2 = PicklerGenerator.generatePickler[TT2]
  // a pickler for TT3 cannot be generated by macro, so use a transform pickler
  implicit val pickler3 = transformPickler[TT3, (Int, String)](t => (t.i, t.s), t => new TT3(t._1, t._2))
  pickler.addConcreteType[TT1].addConcreteType[TT2].addConcreteType[TT3]
}
```

### Complex type hierarchies

When you have more complex type hierarchies with multiple levels of traits, you might need picklers for each type level. A simple example to illustrate:

```scala
sealed trait Element

sealed trait Document extends Element

sealed trait Attribute extends Element

final case class WordDocument(text:String) extends Document

final case class OwnerAttribute(owner: String, parent: Element) extends Attribute
```

Building a `CompositePickler` for `Element` with the two implementation classes doesn't actually give you a pickler for `Document` nor `Attribute`. So
you need to define those picklers separately, duplicating the implementation classes. For this purpose `CompositePickler` allows you to join existing
composite picklers to form a new one.

```scala
object Element {
  implicit val documentPickler = compositePickler[Document]
  documentPickler.addConcreteType[WordDocument]

  implicit val attributePickler = compositePickler[Attribute]
  attributePickler.addConcreteType[OwnerAttribute]

  implicit val elementPickler = compositePickler[Element]
  elementPickler.join[Document].join[Attribute]
}
```

With these picklers you may now pickle any trait. Note, however, that you must use the same `CompositePickler` when unpickling. You cannot pickle with `Element` 
and unpickle with `Attribute` even if the actual class was `OwnerAttribute` because internal indexes are different for each composite pickler.

## References

If your data contains the same object multiple times, BooPickle will encode it only once and use a reference for the remaining occurrences. For example
the data below is correctly unpickled to contain references to the same `p` instances.

```scala
case class Point(x: Int, y: Int)

val p = Point(5, 10)
val points = Vector(p, p, p, p)
val bb = Pickle.intoBytes(points)
.
.
val newPoints = Unpickle[Vector[Point]].fromBytes(bb)
assert(newPoints(0) eq newPoints(1))
```

Reference identity is checked by actual object identity, not by its `equal` method, so a `val a = List(2)` and `val b = List(2)` are two
different objects and will not be replaced by each other in pickling.

## Custom picklers

If you need to pickle non-case classes or for example Java classes, you can define custom picklers for them. If it's a non-generic type,
use an `implicit object` and for generic types use `implicit def`. See `Pickler.scala` for more detailed examples such
as `Either[T, S]` below.

In most cases, however, you can use the `TransformPickler` to create a custom pickler for a type by transforming it into another type that
already has pickler support. For example you can transform a `java.util.Date` into a `Long` and back. More complex classes can be transformed
to a suitable `Tuple`. 

```scala
implicit val datePickler = transformPickler((t: Long) => new java.util.Date(t))(_.getTime)
```

Note that transformation breaks reference equality, so multiple instances of the same reference will be pickled
separately. Transforming picklers can also be used in `CompositePickler` with the `addTransform` method.

For a full pickler you need to do as in the example below. It's optimizing encoding size by reusing the slot for identity reference for
the string length.

```scala
class Custom(val name: String, value: Int)

object MyCustomPicklers extends PicklerHelper {
  implicit object customPickler extends P[Custom] {
    override def pickle(obj: Custom)(implicit state: PickleState): Unit = {
      state.identityRefFor(obj) match {
        case Some(idx) =>
          state.enc.writeInt(-idx)
        case None =>
          // writeString will write an Int with the string length that we can
          // put at same position as identity ref
          state.enc.writeString(obj.name)
          state.enc.writeInt(obj.value)
          state.addIdentityRef(obj)
      }
    }

    override def unpickle(implicit state: UnpickleState): Custom = {
      state.dec.readInt match {
        case idx if idx < 0 =>
          state.identityFor[Custom](-idx)
        case len =>
          val c = new Custom(state.dec.readString(len), state.dec.readInt)
          state.addIdentityRef(c)
          c
      }
    }
  }
}
```

In principle the pickler should do following things:
- check if the object has been pickled already using `state.identityRefFor(obj)` (if you want deduplication)
- if yes, store an index to the reference (this also takes care of `null` values)
- it not, encode the class using `state.enc` and/or calling picklers for members
- if your class has a `length`, you can encode it in the same space as reference index by using a non-negative value
- finally add the object to the identity reference

If your object is immutable, you can use `immutableRefFor` and `addImmutableRef` instead for even more efficient encoding.

On the unpickling side you'll need to do following:
- read reference/length using `state.readInt`
- depending on the result,
  - get an existing reference
  - or use length to know how much to unpickle
- unpickle class members
- finally add the reference to identity table

## Exception picklers

BooPickle has special helpers to simplify pickling most common exception types. A call to `exceptionPickler` gives you a pickler
that supports all the typical Java/Scala exceptions and you can then add your own custom exceptions with `addException`. The exception pickler
is a `CompositePickler[Throwable]` so your exceptions should be presented as `Throwable` to pickling functions.

```scala
implicit val exPickler = exceptionPickler.addException[MyException](m => new MyException(m))

val ex: Throwable = new IllegalArgumentException("No, no, no!")
val bb = Pickle.intoBytes(ex)
```

Note that the basic `addException` mechanism only pickles the exception message, not any other fields. If you wish to pickle more
fields, create transform picklers described above with the `addTransform` function. The same `CompositePickler` can contain both regular
exception picklers and transform picklers.

## Optimizations strategies

### Buffer pooling

When BooPickle codecs allocate `ByteBuffer`s they do it via `BufferProvider` classes. The default implementations for both heap and direct buffers utilize
the `BufferPool` object for recycling buffers. Buffer providers automatically release intermediate `ByteBuffer`s back to the pool when their contents
is copied to a new buffer. To improve pool performance, you should release buffers that are not used anymore by calling `BufferPool.release`. Only
buffers allocated through the `BufferProvider` should be released to the pool.

```scala
val data = Pickle.intoBytes(fruits)
// send data to client
...
// release buffer back to pool
BufferPool.release(data)
```

The pool has a maximum size to prevent it from locking down too much memory and it also only recycles relatively small buffers.

In a multi-threaded environment you may experience some slowdown if multiple threads are actively using `BufferPool`. In these cases it may make sense to
disable pooling globally with `BufferPool.disable()`.

### Deduplication

BooPickle supports deduplication of pickled case classes and strings. If you know your data won't have duplicates, you can enhance performance by disabling it
by setting the `deduplicate` and `dedupImmutable` parameters in `PickleState` and `UnpickleState` constructors to `false`. The effect of deduplication is that
when the same object is encountered again while pickling, only a reference is stored. When unpickling the reference is used instead of unpickling the object
again. This saves space and enhances performance if your data contains a lot of copies of same objects.

There are two different methods of deduplication. First one compares object identities directly and the second compares object contents. The first one can be
used for any objects but the second is safe to use only with immutable objects because only a single instance is created when unpickling and is used for all
references. In the provided picklers immutable deduplication is used only for `String`s, but you can use it in your own picklers if you have immutable data that
is duplicated a lot.

Note that deduplication can severely affect pickling performance (not that much unpickling), especially if you are pickling a lot of non-duplicated objects in
one go.

To implicitly provide non-deduplicating `PickleState` and `UnpickleState`, use following code.

```scala
implicit def pickleState = new PickleState(new EncoderSize, false, false)
implicit val unpickleState = (bb: ByteBuffer) => new UnpickleState(new DecoderSize(bb), false, false)
```

### Codecs

Originally BooPickle had a single codec optimized for both size and speed. From 1.2.0 onwards there are now two codecs, the original one optimized
for size and a new codec optimized for speed (especially in the browser).

The speed oriented codec (`EncodeSpeed` and `DecodeSpeed`) works reliably only within a single application as it may dynamically choose different
encoding methods based on the environment. You should therefore not use it in network communication.

The codec is chosen as part of building an instance of `PickleState` and `UnpickleState`, which implicitly chooses the size optimized codec by
default. To override this you can either manually create the instances of pickle states, or define an implicit to override the defaults. For
`UnpickleState` you need to define a function taking a `ByteBuffer` and returning an instance of `UnpickleState` as in the example below. This will
then be used by the `Unpickle[A].fromBytes` function.

```scala
implicit def pickleState: PickleState = new PickleState(new EncoderSpeed)
implicit val unpickleState = (b: ByteBuffer) => new UnpickleState(new DecoderSpeed(b))
```

### Tuning `ByteBuffer` performance

In the browser BooPickle uses direct `ByteBuffer`s by default, as they perform much better. On the server JVM, however, heap buffers tend to be more
efficient in many cases and are used by default. The `Encoder` constructor takes a `BufferProvider` argument and you can supply your
own or use one of the two predefined ones: `DirectByteBufferProvider` and `HeapByteBufferProvider`. The `ByteBuffer`s *must* use
little-endian ordering.

When serializing large objects, BooPickle encodes them into multiple separate `ByteBuffer`s that are combined (copied) in the call to
`intoBytes`. If you can handle a sequence of buffers (for example sending them over the network), you can use `intoByteBuffers` instead,
which will avoid duplicating the serialized data.

## Performance testing

As one of the main design goals of BooPickle was performance (both in execution speed as in data size), the project includes a sub-project for comparing
BooPickle performance with other common pickling libraries available for Scala.js: uPickle, Prickle, Circe and Pushka. To access the performance tests, just
switch to `perftestsJS` or `perftestsJVM` project.

On the JVM you can run the tests simply with the `run` command and the output will be shown in the SBT console. You might want to run the
test at least twice to ensure JVM has optimized the code properly.

On the JS side, you'll need to use `fullOptJS` and `package` to compile the code into JavaScript and then run it in your browser at
[http://localhost:12345/perftests/js/target/scala-2.11/classes/index.html](http://localhost:12345/perftests/js/target/scala-2.11/classes/index.html) 
To ensure good results, run the tests at least twice in the browser.

Both tests provide similar output, although there are small differences in the Gzipped sizes due to the use of different libraries.

In the browser (BooPickle! is using the speed optimized codec with deduplication disabled):
```
15/16 : Encoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %
BooPickle  80880      41.8%      210        100%       193        100%
BooPickle! 193416     100.0%     402        191%       210        109%
Prickle    9172       4.7%       863        411%       272        141%
uPickle    19372      10.0%      680        324%       233        121%
Circe      10096      5.2%       680        324%       233        121%
Pushka     29176      15.1%      680        324%       233        121%

16/16 : Decoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %
BooPickle  85156      100.0%     210        100%       193        100%
BooPickle! 73576      86.4%      402        191%       210        109%
Prickle    1704       2.0%       863        411%       272        141%
uPickle    8872       10.4%      680        324%       233        121%
Circe      7204       8.5%       680        324%       233        121%
Pushka     18044      21.2%      680        324%       233        121%
```

Under JVM:
```
15/16 : Encoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %
BooPickle  548684     57.6%      210        100%       188        100%
BooPickle! 951836     100.0%     402        191%       205        109%
Prickle    44360      4.7%       879        419%       276        147%
uPickle    139328     14.6%      680        324%       234        124%
Circe      52964      5.6%       680        324%       234        124%
Pushka     151672     15.9%      680        324%       234        124%

16/16 : Decoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %
BooPickle  732512     96.2%      210        100%       188        100%
BooPickle! 761296     100.0%     402        191%       205        109%
Prickle    5308       0.7%       879        419%       276        147%
uPickle    88424      11.6%      680        324%       234        124%
Circe      66248      8.7%       680        324%       234        124%
Pushka     142252     18.7%      680        324%       234        124%
```

Performance test suite measures how many encode or decode operations the library can do in one second and also checks the size of the raw
and gzipped output. Relative speed and size are shown as percentages (bigger is better for speed, smaller is better for size). Typically
BooPickle is 4 to 10 times faster than JSON pickling libraries in decoding and 2 to 5 times faster in encoding.

### Custom tests

You can define your own tests by modifying the `Tests.scala` and `TestData.scala` source files. Just look at the examples provided
and model your own data (as realistically as possible) to see which library works best for you.

## Using BooPickle with Ajax

For now, see [SPA tutorial](https://github.com/ochrons/scalajs-spa-tutorial) for example usage.

## What is it good for?

BooPickle is not a very generic serialization library, so you should think carefully before using it in your application. Typical good and
bad use cases are listed below.

 Good           | Bad 
----------------|-----------------------------------------
Mobile client/server communication | Public API for your service
Data transfer over Websocket binary protocol | Data storage (you will lose it if something changes!)
Scala <-> Scala communication | Scala <-> some-other-language communication
Clients with limited resources | Communication between server components

## Known limitations

BooPickle is first and foremost focused on optimization of the pickled data. This gives you good performance and small data size, but at the same
time it also makes the protocol extremely fragile. Unlike JSON, which can survive quite easily from additional or missing data, the binary
format employed by BooPickle will explode violently with even the slightest of change. Debugging the output of BooPickle is also very hard, first
because it's in binary and second because many data types use exotic coding to reduce the size. For example an `Int` can be 1 to 5 bytes long. Since
there is no type information included in the coding it's quite impossible to determine the structure of the data just by looking at the binary output.

But because there is no type information, it is also possible to benefit from this. For example you can pickle a `Set[String]` but unpickle it
as a `Vector[String]` because all collections use the same serialization format internally. Note, however, that this too is rather fragile, 
especially for empty collections that occur multiple times in the data.

If your data contains a lot of (non-repeating) strings, then BooPickle performance is not so hot (depending on browser) as it has to do
UTF-8 coding itself. Several browsers provide a `TextDecoder` interface to do this efficiently, but it's still not as fast as with `JSON.parse`. On
other browsers, BooPickle relies on Scala.js' implementation for coding UTF-8.

Under Scala.js BooPickle depends indirectly on [typed arrays](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Typed_arrays) 
because direct `ByteBuffer`s are implemented with typed arrays. These may not be available on all JS platforms (most notably old Node.js, which has
its own Buffers, and IE versions 9 and below). When testing code that uses BooPickle (and direct `ByteBuffer`s), make sure your tests are run under
a recent version of Node.js as Rhino doesn't support typed arrays. Alternatively make sure your tests only use heap `ByteBuffer`s.
 
## Common issues with ByteBuffers

As many BooPickle users have run into issues with `ByteBuffers`, here is a bit of advice on how to work with them. If you need to get data out of a 
`ByteBuffer`, for example into an `Array[Byte]` the safest way is to use the `get(array: Array[Byte])` method. Even when the `ByteBuffer` is
backed with an `Array[Byte]` and you could access that directly with `array()`, it's very easy to make mistakes with positions, array offsets and limits.

Reading values from a `ByteBuffer` commonly changes its internal state (the `position`), so you cannot treat it as identical to the original
`ByteBuffer`. Similarly writing to one also changes its state. For example if you write data to a `ByteBuffer` and pass it as such to an unpickler, 
it will not work. You need to call `flip()` first to reset its `position`.

In BooPickle `ByteBuffer`s use little-endian ordering, which is not the default in the JVM, but is the native ordering in majority of target platforms.

For more information, please refer to the [JDK documentation on ByteBuffers](http://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html).

### Using ByteBuffers in network communication

BooPickle is commonly used in client/server communication, so it is important to be able to use `ByteBuffer`s efficiently in the protocol. On the JVM
side things are usually quite simple as many communication methods already accept `ByteBuffer` type directly. Sometimes you do need to convert the
data into an `Array[Byte]` using following piece of code:

```scala
val data = Array.ofDim[Byte](buffer.remaining())
buffer.get(data)
```

Conversion in the other direction is trivial with the help of `ByteBuffer.wrap()` method.

On the JS side things are a bit more complicated due to the use of JavaScript `ArrayBuffer` underneath the `ByteBuffer`.

## Internal details

### Efficient coding

BooPickle makes assumptions of what kind of data it needs to encode, to reach high efficiency in typical scenarios. For example an `Int` (which takes
32-bits or 4 bytes) is encoded in 1-5 bytes depending on the value. The most common values (0-127) take only a single byte, whereas larger values
require more bytes. Because very large integers take 5 bytes, if you know your data consists mainly of such values, you could specifically code them
using `raw` format that always takes 32-bits. Similarly `Long`s are also coded in 1-9 bytes depending on the value.

In many situations there is a need to encode a length (of String, Seq, Map, etc.) and the efficient Int coding is used. But because a length/size is always
non-negative, we can use negative integers to indicate other things. BooPickle supports coding multiple instances of the same object reference by using
a reference value. The length value is reused to encode the reference by just flipping it into a negative value.

Note that when using the speed optimized codecs (`EncodeSpeed` and `DecodeSpeed`) some of these size optimizations are not used.

### Automatic pickler generation with macros

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

The [macro code](https://github.com/ochrons/boopickle/blob/master/boopickle/shared/src/main/scala/boopickle/PicklerMaterializersImpl.scala) starts
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

### Fast UTF-8 coding in the browser

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

## Change history

See a separate [changes document](CHANGES.md)

## Contributors

BooPickle was created and is maintained by [Otto Chrons](https://github.com/ochrons) - otto@chrons.me - Twitter: [@ochrons](https://twitter.com/ochrons).

Special thanks to Li Haoyi and Ben Hutchison for their pickling libraries, which provided more than inspiration to BooPickle.

Contributors: @japgolly, @FlorianKirmaier, @guersam, @akshaal, @cquiroz

## MIT License

Copyright (c) 2015, Otto Chrons (otto@chrons.me)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in 
the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
