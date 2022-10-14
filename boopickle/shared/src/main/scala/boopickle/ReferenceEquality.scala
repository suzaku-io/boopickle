package boopickle

@deprecated("Internal use only. To be removed.", since = "1.4.1")
object ReferenceEquality {
  @inline def eq(a: AnyRef, b: AnyRef): Boolean  = a eq b
  @inline def ne(a: AnyRef, b: AnyRef): Boolean  = a ne b
  @inline def identityHashCode(obj: AnyRef): Int = System.identityHashCode(obj)
}
