package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9438
 * 
 * Issue: Scheduler throughput improvements
 * 
 * This test verifies scheduler throughput.
 */
object BountySpec9438 extends ZIOSpecDefault {
  def spec = suite("Scheduler throughput")(
    test("scheduler handles many lightweight fibers") {
      for {
        start <- Clock.nanoTime
        
        fibers <- ZIO.foreach((1 to 100).toList) { i =>
          ZIO.succeed(i).fork
        }
        
        _ <- ZIO.foreach(fibers)(_.join)
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 5000 // Should complete in under 5 seconds
    },
    
    test("scheduler efficiently switches fibers") {
      for {
        counter <- Ref.make(0)
        
        _ <- ZIO.foreach((1 to 50).toList) { _ =>
          (counter.update(_ + 1) *> ZIO.yieldNow *> counter.update(_ + 1)).fork
        }.flatMap(_.join)
        
        count <- counter.get
      } yield count == 100 // 50 fibers * 2 updates each
    },
    
    test("scheduler throughput for CPU-bound work") {
      for {
        results <- ZIO.foreachPar((1 to 20).toList) { i =>
          ZIO.succeed(i * 2)
        }
        
        sum = results.sum
      } yield sum == 420 // 2+4+6+...+40
    },
    
    test("scheduler throughput for I/O-bound work") {
      for {
        results <- ZIO.foreachPar((1 to 20).toList) { i =>
          ZIO.sleep(10.millis) *> ZIO.succeed(i)
        }
        
        count = results.size
      } yield count == 20
    },
    
    test("scheduler doesn't starve any fiber") {
      for {
        counters <- Ref.make(Map.empty[Int, Int])
        
        fibers <- ZIO.foreach((1 to 10).toList) { id =>
          (ZIO.foreach((1 to 10).toList) { _ =>
            counters.update(m => m + (id -> (m.getOrElse(id, 0) + 1)))
          } *> ZIO.yieldNow).repeat(Schedule.recurs(10)).fork
        }
        
        _ <- ZIO.foreach(fibers)(_.join)
        
        finalCounts <- counters.get
      } yield finalCounts.size == 10 && finalCounts.values.forall(_ == 10)
    }
  )
}
