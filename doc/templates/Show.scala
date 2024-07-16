import java.nio.ByteBuffer

import scalatags.JsDom.all._

trait Show[A] {
  def toHTML(a: A): Modifier
}

private def printByteBuffer(bb: ByteBuffer): List[String] = {
  val data = Array.ofDim[Byte](bb.remaining())
  bb.duplicate().get(data)
  data.grouped(16).toList.map { d =>
    val hex = d.map(c => "%02X " format (c & 0xff)).mkString
    val str = d
      .collect {
        case ascii if ascii >= 0x20 && ascii < 0x80 => ascii
        case _                                      => '.'.toByte
      }
      .map(_.toChar)
      .mkString
    hex.padTo(16 * 3, ' ') + str
  }
}

implicit val showByteBuffer: Show[ByteBuffer] = new Show[ByteBuffer] {
  def toHTML(a: ByteBuffer): Modifier =
    pre(
      style := "background-color: #eee; padding: 10px;",
      printByteBuffer(a).mkString("\n")
    )
}

def show[A](a: A)(implicit s: Show[A]) = Fiddle.print(s.toHTML(a))
