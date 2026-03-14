package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9163
 * 
 * Issue: ZStream.zip should stop when the shortest stream ends
 * 
 * This test verifies zip stops at shortest stream.
 */
object BountySpec9163 extends ZIOSpecDefault {
  def spec = suite("ZStream.zip shortest stream")(
    test("zip stops when shortest stream ends") {
      val stream1 = ZStream(1, 2, 3)
      val stream2 = ZStream(1, 2, 3, 4, 5)
      
      result <- stream1.zip(stream2).runCollect
      
      expected = Chunk((1, 1), (2, 2), (3, 3))
    } yield result == expected
    },
    
    test("zip with equal length streams") {
      val stream1 = ZStream(1, 2, 3, 4, 5)
      val stream2 = ZStream(10, 20, 30, 40, 50)
      
      result <- stream1.zip(stream2).runCollect
      
      expected = Chunk((1, 10), (2, 20), (3, 30), (4, 40), (5, 50))
    } yield result == expected
    },
    
    test("zip stops at first stream if shorter") {
      val stream1 = ZStream(1, 2)
      val stream2 = ZStream(1, 2, 3, 4, 5)
      
      result <- stream1.zip(stream2).runCollect
      
      expected = Chunk((1, 1), (2, 2))
    } yield result == expected
    },
    
    test("zip stops at second stream if shorter") {
      val stream1 = ZStream(1, 2, 3, 4, 5)
      val stream2 = ZStream(10, 20)
      
      result <- stream1.zip(stream2).runCollect
      
      expected = Chunk((1, 10), (2, 20))
    } yield result == expected
    },
    
    test("zip with empty stream") {
      val stream1 = ZStream.empty[Int]
      val stream2 = ZStream(1, 2, 3)
      
      result <- stream1.zip(stream2).runCollect
      
      expected = Chunk.empty[(Int, Int)]
    } yield result == expected
    },
    
    test("zipAll with different lengths") {
      val stream1 = ZStream(1, 2)
      val stream2 = ZStream(10, 20, 30, 40)
      
      result <- stream1.zipAll(0, -1)(stream2).runCollect
      
      expected = Chunk((1, 10), (2, 20), (0, 30), (0, 40))
    } yield result == expected
    }
  )
}
