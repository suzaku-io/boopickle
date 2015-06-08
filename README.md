# BooPickle

BooPickle is the [fastest](http://ochrons.github.io/boopickle-perftest/) and most size efficient serialization (aka pickling) library for Scala 
and [Scala.js](http://www.scala-js.org). It encodes into a binary format instead of the more customary JSON. A binary format brings efficiency 
gains in both size and speed, at the cost of legibility of the encoded data. BooPickle borrows heavily from both [uPickle](https://github.com/lihaoyi/upickle) 
and [Prickle](https://github.com/benhutchison/prickle) so special thanks to Li Haoyi and Ben Hutchison for those two great libraries!

## Features

- Supports both Scala and Scala.js (no reflection!)
- Serialization support for all primitives, collections, options, tuples and case classes (including class hierarchies)
- User-definable custom serializers
- Handles [references and deduplication of identical objects](#references)
- Very fast
- Very efficient coding
- Low memory usage, no intermediate structures needed
- Special optimization for UUID and numeric strings
- Zero dependencies
- Scala 2.11 (no Scala 2.10.x support at the moment)
- All modern browsers are supported (not IE9 and below, though)

## Getting started

Add following dependency declaration to your Scala project 

```scala
"me.chrons" %% "boopickle" % "0.1.3"
```

On a Scala.js project the dependency looks like this

```scala
"me.chrons" %%% "boopickle" % "0.1.3"
```

To use it in your code, simply import the main package contents.

```scala
import boopickle._
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
- common types: `Tuple`s, `Option`, `Either`, `Duration`, `UUID` and `ByteBuffer`
- collections, both mutable and immutable, including: `Vector`, `List`, `Set`s, `Map`s and any `Iterable` with a `CanBuildFrom` implementation
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
above, all you need to do is to define an implicit pickler like this:

```scala
implicit val fruitPickler = CompositePickler[Fruit].addConcreteType[Banana].addConcreteType[Kiwi].addConcreteType[Carambola]
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

### Recursive composite types

If you have a recursive composite type (a sub type has a reference to the super type), you need to build the `CompositePickler` in two steps,
as shown below.

```scala
sealed trait Tree
case object Leaf extends Tree
case class Node(value: Int, children:Seq[Tree]) extends Tree

object Tree {
  implicit val treePickler = CompositePickler[Tree]
  treePickler.addConcreteType[Node].addConcreteType[Leaf.type]
}
```

This is because the compiler must find a pickler for `Tree` when it's building a pickler for `Node`.

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
  implicit val documentPickler = CompositePickler[Document]
  documentPickler.addConcreteType[WordDocument]

  implicit val attributePickler = CompositePickler[Attribute]
  attributePickler.addConcreteType[OwnerAttribute]

  implicit val elementPickler = CompositePickler[Element]
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

BooPickle also supports storing only one instance of immutable objects, but by default this is only supported for `String`s. Couple of common
strings are pre-filled into the reference table, so that they can be used without encoding them even once. These are defined in the
`Constants.scala` file, if you feel a need to add your own there. Just remember that both pickler and unpickler must use exactly the same
initialization data!
 
## Custom picklers

If you need to pickle non-case classes or for example Java classes, you can define custom picklers for them. If it's a non-generic type,
use an `implicit object` and for generic types use `implicit def`. See `Pickler.scala` and `Unpickler.scala` for more detailed examples such
as `Either[T, S]` below.

```scala
type P[A] = Pickler[A]

def write[A](value: A)(implicit state: PickleState, p: P[A]): Unit = p.pickle(value)(state)

implicit def EitherPickler[T: P, S: P]: P[Either[T, S]] = new P[Either[T, S]] {
  override def pickle(obj: Either[T, S])(implicit state: PickleState): Unit = {
    // check if this Either has been pickled already
    state.identityRefFor(obj) match {
      case Some(idx) =>
        // encode index as negative "length"
        state.enc.writeInt(-idx)
      case None =>
        obj match {
          case Left(l) =>
            state.enc.writeInt(EitherLeft)
            write[T](l)
          case Right(r) =>
            state.enc.writeInt(EitherRight)
            write[S](r)
        }
        state.addIdentityRef(obj)
    }
  }
}

type U[A] = Unpickler[A]

def read[A](implicit state: UnpickleState, u: U[A]): A = u.unpickle

implicit def EitherUnpickler[T: U, S: U]: U[Either[T, S]] = new U[Either[T, S]] {
  override def unpickle(implicit state: UnpickleState): Either[T, S] = {
    state.dec.readIntCode match {
      case Right(EitherLeft) =>
        Left(read[T])
      case Right(EitherRight) =>
        Right(read[S])
      case Right(idx) if idx < 0 =>
        state.identityFor[Either[T, S]](-idx)
      case _ =>
        throw new IllegalArgumentException("Invalid coding for Either type")
    }
  }
}
```

In principle the pickler should do following things:
- check if the object has been pickled already using `state.identityRefFor(obj)`
- if yes, store an index to the reference (this also takes care of `null` values)
- it not, encode the class using `state.enc` and/or calling picklers for members
- if your class has a `length`, you can encode it in the same space as reference index by using a non-negative value
- finally add the object to the identity reference

If your object is immutable, you can use `immutableRefFor` and `addImmutableRef` instead for even more efficient encoding.

On the unpickling side you'll need to do following:
- read reference/length/special code using `state.readIntCode`
- depending on the result,
- get an existing reference
- or use length to know how much to unpickle
- or use the special code to determine what to unpickle
- unpickle class members
- finally add the reference to identity table

Again, if you are using immutable refs in pickling, make sure to use them when unpickling as well. These are two different indexes.

## Performance

As one of the main design goals of BooPickle was performance (both in execution speed as in data size), the project includes a sub-project
for comparing BooPickle performance with the two other common pickling libraries, uPickle and Prickle. To access the performance tests, just
switch to `perftestsJS` or `perftestsJVM` project.

On the JVM you can run the tests simply with the `run` command and the output will be shown in the SBT console. You might want to run the
test at least twice to ensure JVM has optimized the code properly.

On the JS side, you'll need to use `fullOptJS` and `package` to compile the code into JavaScript and then run it in your browser at
[http://localhost:12345/perftests/js/target/scala-2.11/classes/index.html](http://localhost:12345/perftests/js/target/scala-2.11/classes/index.html) 
To ensure good results, run the tests at least twice in the browser.

Both tests provide similar output, although there are small differences in the Gzipped sizes due to the use of different libraries.

```
18/18 : Decoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %         
BooPickle  42418      100.0%     194        100%       192        100%      
Prickle    2056       4.8%       863        445%       272        142%      
uPickle    13740      32.4%      680        351%       233        121%  
```

Performance test suite measures how many encode or decode operations the library can do in one second and also checks the size of the raw
and gzipped output. Relative speed and size are shown as percentages (bigger is better for speed, smaller is better for size). Typically
BooPickle is 4 to 10 times faster than uPickle in decoding and 2 to 5 times faster in encoding. Prickle seems to suffer from scalability
issues, leaving it far behind the two other libraries when data sizes grow.

### Custom tests

You can define your own tests by modifying the `Tests.scala` and `TestData.scala` source files. Just look at the examples provided
and model your own data (as realistically as possible) to see which library works best for you.

### Tuning performance

In the browser BooPickle uses direct `ByteBuffer`s by default, as they perform much better. On the server JVM, however, heap buffers tend to be more 
efficient in many cases and are used by default. The `Encoder` constructor takes a `BufferProvider` argument and you can supply your
own or use one of the two predefined ones: `DirectByteBufferProvider` and `HeapByteBufferProvider`. 

When serializing large objects, BooPickle encodes them into multiple separate `ByteBuffer`s that are combined (copied) in the call to
`intoBytes`. If you can handle a sequence of buffers (for example sending them over the network), you can use `intoByteBuffers` instead,
which will avoid duplicating the serialized data.

## Using BooPickle with Ajax

To be documented :)

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
because direct `ByteBuffer`s are implemented with typed arrays. These may not be available on all JS platforms (most notably Node.js, which has its 
own Buffers, and IE versions 9 and below). When testing code that uses BooPickle (and direct `ByteBuffer`s), make sure your tests are run
under PhantomJS, because neither Node.js nor Rhino support typed arrays. Alternatively make sure your tests only use heap `ByteBuffer`s.
 
## Internal details

### Efficient coding

BooPickle makes assumptions of what kind of data it needs to encode, to reach high efficiency in typical scenarios. For example an `Int` (which takes
32-bits or 4 bytes) is encoded in 1-5 bytes depending on the value. The most common values (0-127) take only a single byte, whereas larger values
require more bytes. Because very large integers take 5 bytes, if you know your data consists mainly of such values, you could specifically code them
using `raw` format that always takes 32-bits. Similarly `Long`s are also coded in 1-9 bytes depending on the value.

In many situations there is a need to encode a length (of String, Seq, Map, etc.) and the efficient Int coding is used. But because a length/size is always
non-negative, we can use negative integers to indicate other things. BooPickle supports coding multiple instances of the same object reference by using
a reference value. The length value is reused to encode the reference by just flipping it into a negative value.

In addition to reusing negative values, the multi-byte integer format also allows for special codings. These are used to indicate special values such as
UUID and numeral strings. For example a UUID as a string takes 36 bytes of space, although it only represents a 128-bit (or 16 byte) value. By recognizing
these specific patterns, they can be represented in a more optimal way, saving up to 20 bytes. Of course inspecting string content when encoding makes
it a bit slower, but it's small price to pay for savings in data size.


To be documented
- efficient coding of `Int` and `Long`
- super-position encoding of length, reference index and special codes
- special encoding of UUID and numeral strings
- case class pickler generation via macros
- use of `TextDecoder` and `TextEncoder` when available on JS for efficient UTF-8 decoding/encoding

## Change history

### 0.1.4

- Fixed a bug in decoding strings from a `ByteBuffer` with an array offset

### 0.1.3

- Fixed a bug in byte order when unpickling a `ByteBuffer`
- Enforce byte ordering before unpickling
- `CompositePickler` supports `join` method to pickle deeper type hierarchies
- Use heap `ByteBuffers` on JVM by default, direct on JS for optimal performance

### 0.1.2

- Support for heap and direct byte buffers (and custom ones, too)
- Support for returning a sequence of ByteBuffers instead of a combined one
- Changed to little endian, and updated integer encoding scheme for negative numbers
- Fixed a bug in unpickling a ByteBuffer
- Optimized string decoding in case of heap buffer

### 0.1.1

- Functions in Un/PickleState were private, so macros did not work outside the boopickle package!
- TextEncoder produces Uint8Array which needs to be cast to Int8Array for ByteBuffer to work
- Added pickler for ByteBuffer (mainly to make BooPickle work easily with Autowire)

### 0.1.0

- Initial release

## Contributors

BooPickle was created and is maintained by [Otto Chrons](https://github.com/ochrons) (otto@chrons.me).

Special thanks to Li Haoyi and Ben Hutchison for their pickling libraries, which provided more than inspiration to BooPickle.

Contributors: @japgolly

## MIT License

Copyright (c) 2015, Otto Chrons (otto@chrons.me)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in 
the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
