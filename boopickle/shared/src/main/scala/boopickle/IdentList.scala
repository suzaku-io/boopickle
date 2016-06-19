package boopickle


/**
  * Specialized fast and cheap to initialize identity list for unpickle state identifier refs
  */
abstract class IdentList {
  def apply(idx: Int): AnyRef

  def updated(obj: AnyRef): IdentList
}

object EmptyIdentList extends IdentList {
  override def apply(idx: Int): AnyRef = throw new IndexOutOfBoundsException

  override def updated(obj: AnyRef): IdentList = new IdentList1Plus(obj)
}

private[boopickle] final class IdentList1Plus(o1: AnyRef) extends IdentList {
  import boopickle.IdentList1Plus.Entry
  var last: Entry = new Entry(o1, null)
  var head: Entry = last

  override def apply(idx: Int): AnyRef = {
    var i = 0
    var e = head
    while(i < idx && e != null) {
      i += 1
      e = e.next
    }
    e.obj
  }

  override def updated(obj: AnyRef): IdentList = {
    val e = new Entry(obj, null)
    last.next = e
    last = e
    this
  }
}

object IdentList1Plus {
  private[boopickle] final class Entry(val obj: AnyRef, var next: Entry)
}
