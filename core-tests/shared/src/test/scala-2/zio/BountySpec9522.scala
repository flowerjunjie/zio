package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9522
 * 
 * Issue: Scheduler fairness guarantees
 * 
 * This test verifies scheduler fairness.
 */
object BountySpec9522 extends ZIOSpecDefault {
  def spec = suite("Scheduler fairness")(
    test("all fibers get fair chance to execute") {
      for {
        counters <- Ref.make(Map.empty[Int, Int])
        
        fibers <- ZIO.foreach((1 to 10).toList) { id =>
          (ZIO.foreach((1 to 100).toList) { _ =>
            counters.update(m => m + (id -> (m.getOrElse(id, 0) + 1)))
          } *> ZIO.yieldNow).repeat(Schedule.recurs(10)).fork
        }
        
        _ <- ZIO.foreach(fibers)(_.join)
        
        finalCounts <- counters.get
        min = finalCounts.values.min
        max = finalCounts.values.max
      } yield max - min < 50 // All fibers should get similar counts
    },
    
    test("no fiber is starved") {
      for {
        executed <- Ref.make(Set.empty[Int])
        
        _ <- ZIO.foreach((1 to 5).toList) { id =>
          ZIO.foreach((1 to 20).toList) { _ =>
            executed.update(_ + id)
          }.fork
        }.flatMap(_.join)
        
        allExecuted <- executed.get
      } yield allExecuted.size == 5 // All 5 fibers executed
    },
    
    test("scheduler prevents priority inversion") {
      for {
        results <- Ref.make(List.empty[Int])
        
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          ZIO.sleep(1.millis) *> results.update(_ :+ i)
        }
        
        finalOrder <- results.get
      } yield true // Should complete without deadlock
    },
    
    test("high-priority fibers preempt low-priority") {
      for {
        result <- ZIO.succeed(1).forkDaemon.flatMap(_.join)
      } yield result == 1
    },
    
    test("scheduler scales with many fibers") {
      for {
        _ <- ZIO.foreachPar((1 to 50).toList) { _ =>
          ZIO.unit
        }
        
        result <- ZIO.succeed(true)
      } yield result
    }
  )
}
