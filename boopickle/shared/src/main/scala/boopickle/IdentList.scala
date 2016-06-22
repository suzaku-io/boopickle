package boopickle

import scala.collection.mutable


/**
  * Specialized fast and cheap to initialize identity list for unpickle state identifier refs
  */
abstract class IdentList {
  def apply(idx: Int): AnyRef

  def updated(obj: AnyRef): IdentList
}

object IdentList {

  private[boopickle] final class Entry(val obj: AnyRef, var next: Entry)

  private[boopickle] final val maxSize = 64
}

object EmptyIdentList extends IdentList {
  override def apply(idx: Int): AnyRef = throw new IndexOutOfBoundsException

  override def updated(obj: AnyRef): IdentList = new IdentList1Plus(obj)
}

/**
  * A fast and simple linked list implementation for identifier list
  *
  * @param o1 First object
  */
private[boopickle] final class IdentList1Plus(o1: AnyRef) extends IdentList {
  import boopickle.IdentList.Entry
  var last: Entry = new Entry(o1, null)
  var head: Entry = last
  var count = 1

  override def apply(idx: Int): AnyRef = {
    var i = 0
    var e = head
    while (i < idx && e != null) {
      i += 1
      e = e.next
    }
    if(e == null)
      throw new IndexOutOfBoundsException
    e.obj
  }

  override def updated(obj: AnyRef): IdentList = {
    val e = new Entry(obj, null)
    last.next = e
    last = e
    count += 1
    if (count > IdentList.maxSize)
      new IdentListBig(head)
    else
      this
  }
}

/**
  * A more scalable implementation for an identifier list
  *
  * @param first First entry in a list of entries
  */
private[boopickle] final class IdentListBig(first: IdentList.Entry) extends IdentList {
  // transform the linked list into an array buffer
  val b = mutable.ArrayBuffer.newBuilder[AnyRef]
  b.sizeHint(IdentList.maxSize)
  var e = first
  while (e != null) {
    b += e.obj
    e = e.next
  }
  val entries = b.result()

  override def apply(idx: Int): AnyRef = {
    entries(idx)
  }

  override def updated(obj: AnyRef): IdentList = {
    entries += obj
    this
  }
}

