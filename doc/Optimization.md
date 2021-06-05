# Optimizations strategies

## Buffer pooling

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

## Deduplication

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

## Codecs

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

## Tuning `ByteBuffer` performance

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
[http://localhost:12345/perftests/js/target/scala-2.12/classes/index.html](http://localhost:12345/perftests/js/target/scala-2.12/classes/index.html) 
To ensure good results, run the tests at least twice in the browser.

Both tests provide similar output, although there are small differences in the Gzipped sizes due to the use of different libraries.

In the browser (BooPickle! is using the speed optimized codec with deduplication disabled):
```
15/16 : Encoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %         
BooPickle  78104      41.2%      210        100%       193        100%      
BooPickle! 189536     100.0%     402        191%       210        109%      
uPickle    22824      12.0%      680        324%       233        121%      
Circe      24977      13.2%      680        324%       233        121%      
Play JSON  10560      5.6%       680        324%       233        121%      

16/16 : Decoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %         
BooPickle  149996     100.0%     210        100%       193        100%      
BooPickle! 126592     84.4%      402        191%       210        109%      
uPickle    10790      7.2%       680        324%       233        121%      
Circe      10600      7.1%       680        324%       233        121%      
Play JSON  7821       5.2%       680        324%       233        121%
```

Under JVM:
```
15/16 : Encoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %
BooPickle  628246     57,7%      210        100%       188        100%
BooPickle! 1089448    100,0%     402        191%       205        109%
uPickle    98301      9,0%       680        324%       234        124%
Circe      141695     13,0%      680        324%       234        124%
Play JSON  81771      7,5%       680        324%       234        124%

BufferPool:
  allocations = 31600590
  misses      = 223355

16/16 : Decoding Seq[Book] with numerical IDs
=============================================
Library    ops/s      %          size       %          size.gz    %
BooPickle  737017     96,6%      210        100%       188        100%
BooPickle! 762602     100,0%     402        191%       205        109%
uPickle    65767      8,6%       680        324%       234        124%
Circe      145578     19,1%      680        324%       234        124%
Play JSON  35194      4,6%       680        324%       234        124%
```

Performance test suite measures how many encode or decode operations the library can do in one second and also checks the size of the raw
and gzipped output. Relative speed and size are shown as percentages (bigger is better for speed, smaller is better for size). Typically
BooPickle is 4 to 10 times faster than JSON pickling libraries in decoding and 2 to 5 times faster in encoding.

### Custom tests

You can define your own tests by modifying the `Tests.scala` and `TestData.scala` source files. Just look at the examples provided
and model your own data (as realistically as possible) to see which library works best for you.
