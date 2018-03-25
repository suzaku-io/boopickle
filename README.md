# BooPickle

[![Join the chat at https://gitter.im/ochrons/boopickle](https://badges.gitter.im/suzaku-io/boopickle.svg)](https://gitter.im/suzaku-io/boopickle?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/suzaku-io/boopickle.svg?branch=master)](https://travis-ci.org/suzaku-io/boopickle)
[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.17.svg)](http://www.scala-js.org)

BooPickle is the [fastest](http://ochrons.github.io/boopickle-perftest/) and most size efficient serialization (aka pickling) library that works on both Scala
and [Scala.js](http://www.scala-js.org). It encodes into a binary format instead of the more customary JSON. A binary format brings efficiency 
gains in both size and speed, at the cost of legibility of the encoded data. BooPickle borrows heavily from both [uPickle](https://github.com/lihaoyi/upickle-pprint)
and [Prickle](https://github.com/benhutchison/prickle) so special thanks to Li Haoyi and Ben Hutchison for those two great libraries!

## Features

- Supports both Scala and Scala.js (no reflection!)
- Serialization support for all primitives, collections, options, tuples and case classes (including class hierarchies)
- User-definable custom serializers
- Transforming serializers to simplify serializing non-case classes
- Handles references and deduplication of identical objects
- Very fast
- Very efficient coding
- Low memory usage, no intermediate structures needed
- Zero dependencies
- Scala 2.11/2.12
- All modern browsers are supported (not IE9 and below, though)

## Getting started

Add following dependency declaration to your Scala project 

```scala
"io.suzaku" %% "boopickle" % "1.3.0"
```

On a Scala.js project the dependency looks like this

```scala
"io.suzaku" %%% "boopickle" % "1.3.0"
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

## Documentation

Read the [full documentation](https://boopickle.suzaku.io)

## Change history

See a separate [changes document](CHANGES.md)

## Contributors

BooPickle was created and is maintained by [Otto Chrons](https://github.com/ochrons) - otto@chrons.me - Twitter: [@ochrons](https://twitter.com/ochrons).

Special thanks to Li Haoyi and Ben Hutchison for their pickling libraries, which provided more than inspiration to BooPickle.

Contributors: @japgolly, @FlorianKirmaier, @guersam, @akshaal, @cquiroz, @cornerman, @notxcain
