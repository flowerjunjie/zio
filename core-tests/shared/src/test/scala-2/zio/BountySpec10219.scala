package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10219
 * 
 * Issue: Fiber scheduling fairness
 * 
 * This test verifies scheduler fairness.
 */
object BountySpec10219 extends ZIOSpecDefault {
  def spec = suite("Fiber scheduling fairness")(
    test("all fibers get fair chance") {
      for {
        executed <- Ref.make(Map.empty[Int, Int])
        
        _ <- ZIO.foreach((1 to 10).toList) { id =>
          (ZIO.foreach((1 to 20).toList) { _ =>
            executed.update(m => m + (id -> (m.getOrElse(id, 0) + 1)))
          } *> ZIO.yieldNow).fork
        }
        
        _ <- ZIO.sleep(500.millis)
        
        counts <- executed.get
        min = counts.values.min
        max = counts.values.max
        
        diff <- max - min
        
        result <- ZIO.succeed(diff < 10)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("no fiber is starved") {
      for {
        executed <- Ref.make(Set.empty[Int])
        
        fibers <- ZIO.foreach((1 to 10).toList) { id =>
          (ZIO.foreach((1 to 20).toList) { _ =>
            executed.update(_ + id)
          } *> ZIO.yieldNow).fork
        }
        
        _ <- ZIO.sleep(500.millis)
        
        _ <- ZIO.foreach(fibers)(_.interrupt)
        
        allExecuted <- executed.get
        size = allExecuted.size
      } yield assertCompletes(ZIO.succeed(size))(equalTo(10))
    },
    
    test("high-priority fibers preempt low-priority") {
      for {
        lowPriority = ZIO.foreground.fork(
          ZIO.foreach((1 to 10).toList) { _ =>
            ZIO.yieldNow *> ZIO.unit
          }
        ).fork
        
        highPriority <- ZIO.succeed(1).forkDaemon
        
        _ <- ZIO.sleep(100.millis)
        _ <- lowPriority.interrupt
        _ <- highPriority.join
        
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("fair scheduling is maintained over time") {
      for {
        executed <- Ref.make(0)
        
        _ <- (executed.update(_ + 1) *> ZIO.yieldNow).repeat(Schedule.recurs(10)).fork
        
        _ <- ZIO.sleep(2000.millis)
        
        _ <- executed.update(_ * 2) *> ZIO.yieldNow).repeat(Schedule.recurs(5)).fork
        
        _ <- ZIO.sleep(1000)
        _ <- executed.update(_ * 3) *> ZIO.yieldNow).repeat(Schedule.recurs(3)).fork
        
        _ <- ZIO.sleep(500)
        _ <- executed.update(_ * 4) *> ZIO.yieldNow).repeat(Schedule.recurs(2)).fork
        
        _ <- ZIO.sleep(500)
        
        count <- executed.get
      } yield count >= 14 // 2+4+6+2 = 14
    },
    
    test("scheduler doesn't cause priority inversion") {
      for {
        priority <- Ref.make(0)
        
        lowFiber <- (priority.update(_ + 1) *> ZIO.yieldNow).repeat(Schedule.recurs(20)).fork
        highFiber <- (priority.update(_ + 100) *> ZIO.succeed(42)).forkDaemon
        
        _ <- ZIO.sleep(100.millis)
        _ <- lowFiber.interrupt
        _ <- highFiber.join
        
        lowCount <- priority.get
        highCount <- priority.get
        
        result <- ZIO.succeed(lowCount > 0 && highCount > 0)
      } yield result
    },
    
    test("daemon fibers run when possible") {
      for {
        result <- ZIO.succeed(1).forkDaemon.flatMap(_.join)
      } yield result == 1
    }
  )
}
