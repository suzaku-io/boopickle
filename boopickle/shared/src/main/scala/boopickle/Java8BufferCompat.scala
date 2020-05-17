package boopickle

import java.nio.Buffer

/**
 * Force linking to Buffer methods instead of ByteBuffer overloads
 * to retain Java 8 compatibility
 */
private[boopickle] object Java8BufferCompat {

  def flip(bb: Buffer): Unit = bb.flip()

  def position(bb: Buffer, newPosition: Int): Unit = bb.position(newPosition)

  def clear(bb: Buffer): Unit = bb.clear()

  def mark(bb: Buffer): Unit = bb.mark()

  def reset(bb: Buffer): Unit = bb.reset()
}
