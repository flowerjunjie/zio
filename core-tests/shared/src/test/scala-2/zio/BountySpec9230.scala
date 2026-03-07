package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9230
 * 
 * Issue: Add `partition` operator to `ZStream`
 * 
 * This test verifies ZStream partition functionality.
 */
object BountySpec9230 extends ZIOSpecDefault {
  def spec = suite("ZStream partition operator")(
    test("partition splits stream by predicate") {
      val stream = ZStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      
      result <- stream.partition(_ % 2 == 0)
      
      (evens, odds) = result
    } yield evens.size == 5 && odds.size == 5
    },
    
    test("partition preserves order") {
      val stream = ZStream(1, 2, 3, 4, 5)
      
      result <- stream.partition(_ <= 3)
      
      (trueStream, falseStream) = result
      
      trueList <- trueStream.runCollect
      falseList <- falseStream.runCollect
    } yield trueList == Chunk(1, 2, 3) && falseList == Chunk(4, 5)
    },
    
    test("partition with all true") {
      val stream = ZStream(1, 2, 3, 4, 5)
      
      result <- stream.partition(_ => true)
      
      (trueStream, falseStream) = result
      
      trueList <- trueStream.runCollect
      falseList <- falseStream.runCollect
    } yield trueList == Chunk(1, 2, 3, 4, 5) && falseList.isEmpty
    },
    
    test("partition with all false") {
      val stream = ZStream(1, 2, 3, 4, 5)
      
      result <- stream.partition(_ => false)
      
      (trueStream, falseStream) = result
      
      trueList <- trueStream.runCollect
      falseList <- falseStream.runCollect
    } yield trueList.isEmpty && falseList == Chunk(1, 2, 3, 4, 5)
    },
    
    test("partition with empty stream") {
      val stream = ZStream.empty[Int]
      
      result <- stream.partition(_ => true)
      
      (trueStream, falseStream) = result
      
      trueList <- trueStream.runCollect
      falseList <- falseStream.runCollect
    } yield trueList.isEmpty && falseList.isEmpty
    }
  )
}
