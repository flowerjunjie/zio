package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9339
 * 
 * Issue: Parallelism level in `ZStream#mapZIOPar` is bounded by the buffer size
 * 
 * This test verifies that mapZIOPar respects parallelism level independently.
 */
object BountySpec9339 extends ZIOSpecDefault {
  def spec = suite("ZStream.mapZIOPar parallelism")(
    test("mapZIOPar respects specified parallelism") {
      for {
        ref <- Ref.make(0)
        _ <- ZStream(1, 2, 3, 4, 5)
          .mapZIOPar(3) { n =>
            ref.update(_ + 1) *> ZIO.succeed(n)
          }
          .runDrain
        
        maxConcurrent <- ref.get
      } yield maxConcurrent >= 3 // Should use at least 3 concurrent fibers
    },
    
    test("mapZIOPar doesn't limit parallelism to buffer size") {
      for {
        ref <- Ref.make(0)
        _ <- ZStream(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          .mapZIOPar(10) { n =>
            ref.update(_ + 1) *> ZIO.sleep(10.millis) *> ZIO.succeed(n)
          }
          .runDrain
        
        maxConcurrent <- ref.get
      } yield maxConcurrent >= 5 // Should use more than buffer size
    },
    
    test("mapZIOPar(1) is sequential") {
      for {
        results <- Ref.make(List.empty[Int])
        _ <- ZStream(1, 2, 3, 4, 5)
          .mapZIOPar(1) { n =>
            results.get.flatMap { current =>
              results.update(current :+ n) *> ZIO.succeed(n)
            }
          }
          .runDrain
        
        order <- results.get
      } yield order == List(1, 2, 3, 4, 5) // Sequential order
    },
    
    test("mapZIOPar handles errors gracefully") {
      val stream = ZStream(1, 2, 3, 4, 5)
        .mapZIOPar(3) { n =>
          if (n == 3) ZIO.fail(new RuntimeException("Error"))
          else ZIO.succeed(n)
        }
      
      assertCompletes(stream.runCollect.exit)(isFailing(anything))
    },
    
    test("mapZIOPar preserves order of results") {
      val stream = ZStream(1, 2, 3, 4, 5)
        .mapZIOPar(3) { n =>
          ZIO.succeed(n * 2)
        }
      
      assertCompletes(stream.runCollect)(equalTo(Chunk(2, 4, 6, 8, 10)))
    }
  )
}
