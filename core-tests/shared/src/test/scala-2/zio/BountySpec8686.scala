package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #8686
 * 
 * Issue: ZStream.groupedWithin works in unexpected way for infinite stream
 * 
 * This test verifies groupedWithin behavior with infinite streams.
 */
object BountySpec8686 extends ZIOSpecDefault {
  def spec = suite("ZStream.groupedWithin infinite stream")(
    test("groupedWithin handles infinite streams correctly") {
      val infiniteStream = ZStream.iterate(0)(_ + 1)
      
      // Should not hang or consume infinite memory
      val stream = infiniteStream.groupedWithin(100, 1.second)
      
      assertCompletes(stream.take(1).runCollect)(hasSize(equalTo(1)))
    },
    
    test("groupedWithin respects time window for infinite streams") {
      val infiniteStream = ZStream.iterate(0)(_ + 1)
      
      val stream = infiniteStream.groupedWithin(1000, 100.millis)
      
      // Should emit at least one group within time window
      assertCompletes(stream.take(1).runCollect)(hasSize(equalTo(1)))
    },
    
    test("groupedWithin doesn't buffer entire infinite stream") {
      val infiniteStream = ZStream.iterate(0)(_ + 1)
      
      val stream = infiniteStream.groupedWithin(10, 1.second)
      
      // Should emit groups without consuming infinite stream
      assertCompletes(stream.take(5).runCollect)(hasSize(equalTo(5)))
    },
    
    test("groupedWithin(1, ...) behaves correctly for infinite streams") {
      val infiniteStream = ZStream.iterate(0)(_ + 1)
      
      val stream = infiniteStream.groupedWithin(1, 1.second)
      
      // Each group should have exactly 1 element
      result <- stream.take(3).runCollect
      
      sizes = result.map(_.size)
    } yield sizes == Chunk(1, 1, 1)
  )
}
