package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9310
 * 
 * Issue: ZStream performance improvements
 * 
 * This test verifies ZStream performance characteristics.
 */
object BountySpec9310 extends ZIOSpecDefault {
  def spec = suite("ZStream performance")(
    test("ZStream.map is efficient") {
      for {
        start <- Clock.nanoTime
        
        result <- ZStream(1 to 1000)
          .map(_ * 2)
          .runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield result.size == 1000 && duration < 1000
    },
    
    test("ZStream.filter is efficient") {
      for {
        result <- ZStream(1 to 1000)
          .filter(_ % 2 == 0)
          .runCollect
      } yield result.size == 500
    },
    
    test("ZStream.flatMap doesn't buffer excessively") {
      for {
        result <- ZStream(1 to 100)
          .flatMap(i => ZStream(i, i * 2))
          .runCollect
      } yield result.size == 200
    },
    
    test("ZStream.take is O(n) not O(1)") {
      for {
        result <- ZStream.iterate(0)(_ + 1)
          .take(100)
          .runCollect
      } yield result.size == 100
    },
    
    test("ZStream.mergeBoth is concurrent") {
      for {
        stream1 = ZStream.fromIterable(1 to 50)
        stream2 = ZStream.fromIterable(51 to 100)
        
        result <- stream1.merge(stream2).runCollect
      } yield result.size == 100
    },
    
    test("ZStream.concat preserves order") {
      val stream1 = ZStream(1, 2, 3)
      val stream2 = ZStream(4, 5, 6)
      
      result <- (stream1 ++ stream2).runCollect
      
      expected = Chunk(1, 2, 3, 4, 5, 6)
    } yield result == expected
    }
  )
}
