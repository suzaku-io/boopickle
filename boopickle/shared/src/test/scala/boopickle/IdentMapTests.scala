package boopickle

import boopickle.IdentMap3Plus.Entry
import utest._

object IdentMapTests extends TestSuite {
  override def tests = Tests {
    'IdentMap - {
      'empty {
        val m = IdentMap.empty
        assert(m("test").isEmpty)
      }
      'single {
        val test = Option(42)
        val m    = EmptyIdentMap.updated(test)
        val x    = m(test)
        assert(x.contains(2))
      }
      'hash {
        val objs = for (i <- 0 until 60) yield Option(i)
        var m    = IdentMap.empty
        objs.tail.foreach(o => m = m.updated(o))
        val entries = m.asInstanceOf[IdentMap3Plus].hashTable.toSeq
        def entrySize(entry: Entry): Int = {
          if (entry == null)
            0
          else
            1 + entrySize(entry.next)
        }
        val tableSize = entries.map(entrySize)
        println(tableSize)
        assert(tableSize.forall(s => s >= 0 && s < 8))
      }
      'big {
        val objs = for (i <- 0 until 1000) yield s"id$i"
        var m    = IdentMap.empty
        objs.foreach(o => m = m.updated(o))
        assert(m(objs(500)).contains(502))
      }
      'huge {
        val objs = for (i <- 0 until 16384) yield s"id$i"
        var m    = IdentMap.empty
        objs.foreach(o => m = m.updated(o))
        println(m(objs(500)))
        assert(m(objs(500)).contains(502))
        assert(m(objs(16000)).contains(16002))
      }
    }
  }
}
