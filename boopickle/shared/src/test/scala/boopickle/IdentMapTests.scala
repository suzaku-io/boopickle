package boopickle

import utest._

object IdentMapTests  extends TestSuite {
  override def tests = TestSuite {
    'IdentMap - {
      'empty - {
        val m = IdentMap.empty
        assert(m("test").isEmpty)
      }
      'single - {
        val test = Option(42)
        val m = EmptyIdentMap.updated(test)
        val x = m(test)
        assert(x.contains(2))
      }
      'hash - {
        val objs = for( i <- 0 until 1024) yield Option(i)
        var m: IdentMap = EmptyIdentMap
        objs.tail.foreach(o => m = m.updated(o))
        val entries = m.asInstanceOf[NonEmptyIdentMap].hashTable.toSeq
        def entrySize(entry: Entry): Int = {
          if(entry == null)
            0
          else
            1 + entrySize(entry.next)
        }
        entries.map(entrySize).forall(s => s > 4 && s < 64)
      }
    }
  }
}
