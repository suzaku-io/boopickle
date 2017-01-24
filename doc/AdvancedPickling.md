# Advanced pickling

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

