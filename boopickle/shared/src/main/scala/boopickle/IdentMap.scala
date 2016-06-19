package boopickle

/**
  * Specialized fast and cheap to initialize identity map for pickle state identifier map
  */
abstract class IdentMap {
  def apply(obj: AnyRef): Option[Int]

  def updated(obj: AnyRef): IdentMap
}

object EmptyIdentMap extends IdentMap {
  override def apply(obj: AnyRef): Option[Int] = None

  override def updated(obj: AnyRef): IdentMap = new IdentMap1(obj)
}

private[boopickle] final class IdentMap1(o1: AnyRef) extends IdentMap {
  override def apply(obj: AnyRef): Option[Int] = {
    if (obj eq o1)
      Some(2)
    else None
  }

  override def updated(obj: AnyRef): IdentMap = new IdentMap2(o1, obj)
}

private[boopickle] final class IdentMap2(o1: AnyRef, o2: AnyRef) extends IdentMap {
  override def apply(obj: AnyRef): Option[Int] = {
    if (obj eq o1)
      Some(2)
    else if (obj eq o2)
      Some(3)
    else None
  }

  override def updated(obj: AnyRef): IdentMap = new IdentMap3Plus(o1, o2, obj)
}

object IdentMap3Plus {

  private[boopickle] class Entry(val hash: Int, val obj: AnyRef, val idx: Int, val next: Entry)

}

private[boopickle] final class IdentMap3Plus(o1: AnyRef, o2: AnyRef, o3: AnyRef) extends IdentMap {
  import IdentMap3Plus.Entry

  val hashSize = 32
  val hashTable = new Array[Entry](hashSize)
  // indices 0 (not used) and 1 (for null) are reserved
  var curIdx = 2

  // initialize with data
  updated(o1)
  updated(o2)
  updated(o3)

  @inline private def hashIdx(hash: Int) = {
    val a = (hash >> 16) ^ hash
    val b = (a >> 8) ^ a
    ((b >> 3) ^ b) & (hashSize - 1)
  }

  override def apply(obj: AnyRef): Option[Int] = {
    val hash = System.identityHashCode(obj)
    val tableIdx = hashIdx(hash)
    var e = hashTable(tableIdx)
    while ((e != null) && (e.hash != hash) && (e.obj ne obj))
      e = e.next
    if (e == null)
      None
    else
      Some(e.idx)
  }

  override def updated(obj: AnyRef): IdentMap = {
    val hash = System.identityHashCode(obj)
    val tableIdx = hashIdx(hash)
    hashTable(tableIdx) = new Entry(hash, obj, curIdx, hashTable(tableIdx))
    curIdx += 1
    this
  }
}

object IdentMap {
  def empty = EmptyIdentMap
}
