# Getting started

Add following dependency declaration to your Scala project:

<pre><code class="lang-scala">"io.suzaku" %% "boopickle" % "{{ book.version }}"</code></pre>

On a Scala.js or a Scala Native project the dependency looks like this:

<pre><code class="lang-scala">"io.suzaku" %%% "boopickle" % "{{ book.version }}"</code></pre>

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

BooPickle has built-in support for most of the regular Scala types, including:

- primitives: `Boolean`, `Byte`, `Short`, `Char`, `Int`, `Long`, `Float`, `Double` and `String`
- common types: `Tuple`s, `Option`, `Either`, `Duration`, `UUID`, `BigInt`, `BigDecimal` and `ByteBuffer`
- collections, both mutable and immutable, including: `Array`, `Vector`, `List`, `Set`s, `Map`s and any `Iterable` with a `CanBuildFrom` implementation
- `case class`es and `case object`s (via a macro)
- `trait`s as a base for a class hierarchy

