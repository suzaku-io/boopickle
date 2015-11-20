package boopickle.perftests

import java.util.UUID

import scala.util.Random
import pushka.annotation._

@pushka
case class Book(id: String, name: String, author: String, publicationYear: Int)

@pushka
case class Node(name: String, color: String, children: Seq[Node])

object TestData {
  val uuids = {
    val r = new Random(0)
    (0 until 100).map(_ => new UUID(r.nextLong(), r.nextLong()).toString)
  }
  var uuidIdx = 0
  def genUUID = {
    uuidIdx = (uuidIdx + 1) % uuids.size
    uuids(uuidIdx)
  }

  def books(idGen: => String): Seq[Book] = Seq(
    Book(idGen, "Carrie", "Stephen King", 1974),
    Book(idGen, "'Salem's Lot", "Stephen King", 1975),
    Book(idGen, "Rage	Bachman", "Stephen King", 1976),
    Book(idGen, "Shining, The", "Stephen King", 1976),
    Book(idGen, "Last Rung on the Ladder", "Stephen King", 1977),
    Book(idGen, "Böögeyman", "Stephen King", 1977),
    Book(idGen, "Cat from Hell", "Stephen King", 1977),
    Book(idGen, "Man Who Loved Flowers", "Stephen King", 1977)
  )

  def genNumId = {
    uuidIdx = (uuidIdx + 1) % uuids.size
    (uuidIdx + 1000).toString
  }

  def genRandomId = {
    uuidIdx = (uuidIdx + 1) % uuids.size
    "R" + uuids(uuidIdx).replaceAll("-", ":").substring(0, 10)
  }

  val booksNumId = books(genNumId)

  val booksUUID = books(genUUID)

  val booksRandomID = books(genRandomId)

  val largeIntSeq: Seq[Int] = {
    val r = new Random(0)
    for (i <- 0 until 10000) yield (1.0 / (1.0 + r.nextDouble() * 1e5) * 1e7).toInt
  }

  val largeStringIntMap: Map[String, Int] = {
    val r = new Random(0)
    var map = Map.empty[String, Int]
    for (i <- 0 until 10000) map += s"ID$i" -> (1.0 / (1.0 + r.nextDouble() * 1e5) * 1e7).toInt
    map
  }

  val largeFloatSeq: Seq[Float] = {
    val r = new Random(0)
    for (i <- 0 until 1000) yield ((r.nextDouble() - 0.1) * 1e6).toFloat
  }

  val largeDoubleSeq: Seq[Double] = {
    val r = new Random(0)
    for (i <- 0 until 1000) yield (r.nextDouble() - 0.1) * 1e6
  }

  val colors = Seq("black", "red", "white", "yellow", "blue")

  def genTree(maxChildren: Int, maxDepth: Int, r: Random = new Random(0)): Node = {
    val childrenCount = r.nextInt(maxChildren + maxDepth) max maxDepth min maxChildren
    Node(genRandomId, colors(r.nextInt(colors.size)), Vector.tabulate(childrenCount)(_ => genTree(maxChildren, maxDepth - 1, r)))
  }
}
