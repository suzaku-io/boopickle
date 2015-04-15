package boopickle

import utest._

import scala.collection.mutable

object PickleTests extends TestSuite {
  override def tests = TestSuite {
    'Boolean - {
      'true {
        val bb = Pickle.intoBytes(true)
        assert(bb.limit == 1)
        assert(Unpickle[Boolean].fromBytes(bb))
      }
      'false {
        val bb = Pickle.intoBytes(false)
        assert(bb.limit == 1)
        assert(!Unpickle[Boolean].fromBytes(bb))
      }
    }
    'Byte - {
      'zero {
        val bb = Pickle.intoBytes(0.toByte)
        assert(bb.limit == 1)
        assert(Unpickle[Byte].fromBytes(bb) == 0)
      }
      'positive {
        val bb = Pickle.intoBytes(50.toByte)
        assert(bb.limit == 1)
        assert(Unpickle[Byte].fromBytes(bb) == 50)
      }
      'negative {
        val bb = Pickle.intoBytes(-1.toByte)
        assert(bb.limit == 1)
        assert(Unpickle[Byte].fromBytes(bb) == -1)
      }
    }
    'Short - {
      'zero {
        val bb = Pickle.intoBytes(0.toShort)
        assert(bb.limit == 1)
        assert(Unpickle[Short].fromBytes(bb) == 0)
      }
      'positive {
        val bb = Pickle.intoBytes(0x4000.toShort)
        assert(bb.limit == 3)
        assert(Unpickle[Short].fromBytes(bb) == 0x4000)
      }
      'negative {
        val bb = Pickle.intoBytes(-1.toShort)
        assert(bb.limit == 2)
        assert(Unpickle[Short].fromBytes(bb) == -1)
      }
    }
    'Int - {
      'zero {
        val bb = Pickle.intoBytes(0)
        assert(bb.limit == 1)
        assert(Unpickle[Int].fromBytes(bb) == 0)
      }
      'positive {
        val bb = Pickle.intoBytes(1024)
        assert(bb.limit == 2)
        assert(Unpickle[Int].fromBytes(bb) == 1024)
      }
      'negative {
        val bb = Pickle.intoBytes(-1048577)
        assert(bb.limit == 4)
        assert(Unpickle[Int].fromBytes(bb) == -1048577)
      }
      'max {
        val bb = Pickle.intoBytes(Int.MaxValue)
        assert(bb.limit == 5)
        assert(Unpickle[Int].fromBytes(bb) == Int.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Int.MinValue)
        assert(bb.limit == 5)
        assert(Unpickle[Int].fromBytes(bb) == Int.MinValue)
      }
    }
    'Long - {
      'positive {
        val bb = Pickle.intoBytes(1024L * 1024L * 1024L * 1024L)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == 1024L * 1024L * 1024L * 1024L)
      }
      'negative {
        val bb = Pickle.intoBytes(-1024L * 1024L * 1024L * 1024L)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == -1024L * 1024L * 1024L * 1024L)
      }
      'max {
        val bb = Pickle.intoBytes(Long.MaxValue)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == Long.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Long.MinValue)
        assert(bb.limit == 9)
        assert(Unpickle[Long].fromBytes(bb) == Long.MinValue)
      }
    }
    'Float - {
      'positive {
        val bb = Pickle.intoBytes(0.185f)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == 0.185f)
      }
      'negative {
        val bb = Pickle.intoBytes(-0.185f)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == -0.185f)
      }
      'max {
        val bb = Pickle.intoBytes(Float.MaxValue)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == Float.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Float.MinValue)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == Float.MinValue)
      }
      'infinity {
        val bb = Pickle.intoBytes(Float.PositiveInfinity)
        assert(bb.limit == 4)
        assert(Unpickle[Float].fromBytes(bb) == Float.PositiveInfinity)
      }
    }
    'Double - {
      'positive {
        val bb = Pickle.intoBytes(math.Pi)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == math.Pi)
      }
      'negative {
        val bb = Pickle.intoBytes(-math.Pi)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == -math.Pi)
      }
      'max {
        val bb = Pickle.intoBytes(Double.MaxValue)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == Double.MaxValue)
      }
      'min {
        val bb = Pickle.intoBytes(Double.MinValue)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == Double.MinValue)
      }
      'infinity {
        val bb = Pickle.intoBytes(Double.PositiveInfinity)
        assert(bb.limit == 8)
        assert(Unpickle[Double].fromBytes(bb) == Double.PositiveInfinity)
      }
    }
    'String - {
      'null {
        val str: String = null
        val bb = Pickle.intoBytes(str)
        assert(bb.limit == 2)
        assert(Unpickle[String].fromBytes(bb) == null)
      }
      'empty {
        val bb = Pickle.intoBytes("")
        assert(bb.limit == 1)
        assert(Unpickle[String].fromBytes(bb) == "")
      }
      'normal {
        val bb = Pickle.intoBytes("normal")
        assert(bb.limit == 1 + 6)
        assert(Unpickle[String].fromBytes(bb) == "normal")
      }
    }
    'Option - {
      'some {
        val bb = Pickle.intoBytes(Some("test"))
        assert(bb.limit == 1 + 5)
        assert(Unpickle[Option[String]].fromBytes(bb).contains("test"))
      }
      'none {
        val d: Option[String] = None
        val bb = Pickle.intoBytes(d)
        assert(bb.limit == 2)
        assert(Unpickle[Option[String]].fromBytes(bb).isEmpty)
      }
    }
    'Seq - {
      'empty {
        val bb = Pickle.intoBytes(Seq.empty[Int])
        assert(bb.limit == 1)
        assert(Unpickle[Seq[Int]].fromBytes(bb) == Seq.empty[Int])
      }
      'singleEntry {
        val bb = Pickle.intoBytes(Seq(4095))
        assert(bb.limit == 3)
        assert(Unpickle[Seq[Int]].fromBytes(bb) == Seq(4095))
      }
      'seqOfSeq {
        val bb = Pickle.intoBytes(Seq(Seq(1), Seq(1, 2)))
        assert(bb.limit == 1 + 2 + 3) // seq length, first subseq, second subseq
        assert(Unpickle[Seq[Seq[Int]]].fromBytes(bb) == Seq(Seq(1), Seq(1, 2)))
      }
      'dupStrings {
        val bb = Pickle.intoBytes(Seq("test", "test", "test", "test"))
        assert(bb.limit == 1 + 5 + 3 * 2) // seq length, first "test", 3x reference
        assert(Unpickle[Seq[String]].fromBytes(bb) == Seq("test", "test", "test", "test"))
      }
    }
    'Map - {
      'empty {
        val bb = Pickle.intoBytes(Map.empty[String, Int])
        assert(bb.limit == 1)
        assert(Unpickle[Map[String, Int]].fromBytes(bb) == Map.empty[String, Int])
      }
      'simple {
        val bb = Pickle.intoBytes(Map(1 -> 2))
        assert(bb.limit == 3)
        assert(Unpickle[Map[Int, Int]].fromBytes(bb) == Map(1 -> 2))
      }
      'mutable {
        val bb = Pickle.intoBytes(mutable.HashMap(1 -> 2))
        assert(bb.limit == 3)
        assert(Unpickle[mutable.HashMap[Int, Int]].fromBytes(bb) == mutable.HashMap(1 -> 2))
      }
    }
    'Set - {
      'empty {
        val bb = Pickle.intoBytes(Set.empty[String])
        assert(bb.limit == 1)
        assert(Unpickle[Set[String]].fromBytes(bb) == Set.empty[String])
      }
      'simple {
        val bb = Pickle.intoBytes(Set(1, 2, -1))
        assert(bb.limit == 1 + 1 + 1 + 2)
        assert(Unpickle[Set[Int]].fromBytes(bb) == Set(1, 2, -1))
      }
    }
    'Array - {
      'empty {
        val bb = Pickle.intoBytes(Array.empty[Int])
        assert(bb.limit == 1)
        assert(Unpickle[Array[Int]].fromBytes(bb) sameElements Array.empty[Int])
      }
      'simple {
        val bb = Pickle.intoBytes(Array(0, 1, -1, -5555))
        assert(bb.limit == 1 + 1 + 1 + 2 + 3)
        assert(Unpickle[Array[Int]].fromBytes(bb) sameElements Array(0, 1, -1, -5555))
      }
      'arrayOfArray {
        val bb = Pickle.intoBytes(Array(Array(0, 0, 0), Array(1, 1, 1)))
        assert(bb.limit == 1 + 4 + 4)
        assert(Unpickle[Array[Array[Int]]].fromBytes(bb).deep == Array(Array(0, 0, 0), Array(1, 1, 1)).deep)
      }
    }
    'IdentityDeduplication - {
      'Seq - {
        val data = Seq(1, 2, 3, 4, 5)
        val bb = Pickle.intoBytes(Seq(data, data, data))
        assert(bb.limit == 1 + 1 + 5 + 2 * 2) // seq len, first instance, 2x reference
        assert(Unpickle[Seq[Seq[Int]]].fromBytes(bb) == Seq(data, data, data))
      }
    }
  }
}
