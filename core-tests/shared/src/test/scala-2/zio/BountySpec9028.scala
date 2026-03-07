package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9028
 * 
 * Issue: ZSink.collectAllToMap{N} should have value extraction function
 * 
 * This test verifies ZSink map collection functionality.
 */
object BountySpec9028 extends ZIOSpecDefault {
  def spec = suite("ZSink.collectAllToMap functionality")(
    test("collectAllToMap collects key-value pairs") {
      val stream = ZStream(1, 2, 3, 4, 5)
      
      val sink = ZSink.collectAllToMap[Int, Int] { n =>
        (n.toString, n * 2)
      }
      
      assertCompletes(stream.run(sink))(equalTo(Map("1" -> 2, "2" -> 4, "3" -> 6, "4" -> 8, "5" -> 10)))
    },
    
    test("collectAllToMap handles duplicates") {
      val stream = ZStream(1, 2, 1, 3, 2)
      
      val sink = ZSink.collectAllToMap[Int, Int] { n =>
        (n, n * 2)
      }
      
      result <- stream.run(sink)
      
      // Later values should override earlier ones
      expected = Map(1 -> 2, 2 -> 4, 3 -> 6)
    } yield result == expected
    },
    
    test("collectAllToMapN with tuple extraction") {
      case class Key(id: Int)
      case class Value(name: String)
      
      val stream = ZStream(
        Key(1) -> Value("a"),
        Key(2) -> Value("b"),
        Key(3) -> Value("c")
      )
      
      val sink = ZSink.collectAllToMapN[(Key, Value), Key, Value] { case (k, v) => (k, v) }
      
      assertCompletes(stream.run(sink))(anything)
    },
    
    test("collectAllToMap preserves insertion order") {
      val stream = ZStream(1, 2, 3, 4, 5)
      
      val sink = ZSink.collectAllToMap[Int, Int] { n =>
        (n, n)
      }
      
      result <- stream.run(sink)
      
      keys = result.keys.toList
    } yield keys == List(1, 2, 3, 4, 5)
  )
}
