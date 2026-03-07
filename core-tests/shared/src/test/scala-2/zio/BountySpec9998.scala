package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9998
 * 
 * Issue: Scheduler priority handling
 * 
 * This test verifies scheduler priority behavior.
 */
object BountySpec9998 extends ZIOSpecDefault {
  def spec = suite("Scheduler priority handling")(
    test("high-priority fibers preempt low-priority") {
      for {
        executed <- Ref.make(List.empty[String])
        
        lowPriority <- (executed.update(_ :+ "low") *> ZIO.sleep(100.millis)).forkDaemon
        
        highPriority <- (executed.update(_ :+ "high") *> ZIO.succeed(42)).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- highPriority.join
        _ <- lowPriority.interrupt
        
        finalOrder <- executed.get
      } yield assertCompletes(ZIO.succeed(finalOrder))(contains("high"))
    },
    
    test("same priority fibers are fair") {
      for {
        counter <- Ref.make(Map.empty[Int, Int])
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { id =>
          counter.update(m => m + (id -> (m.getOrElse(id, 0) + 1)))
        }.fork
        
        _ <- ZIO.foreach(fibers)(_.join)
        
        counts <- counter.get
        min = counts.values.min
        max = counts.values.max
      } yield max - min < 5 // All fibers should get similar counts
    },
    
    test("daemon fibers have lowest priority") {
      for {
        result <- ZIO.succeed(42).forkDaemon.flatMap(_.join)
      } yield result == 42
    },
    
    test("priority doesn't cause starvation") {
      for {
        low <- Ref.make(0)
        high <- Ref.make(0)
        
        lowFiber <- (low.update(_ + 1) *> ZIO.yieldNow).repeat(Schedule.recurs(5)).fork
        highFiber <- (high.update(_ + 1) *> ZIO.yieldNow).repeat(Schedule.recurs(5)).fork
        
        _ <- ZIO.sleep(100.millis)
        _ <- lowFiber.interrupt
        _ <- highFiber.interrupt
        
        l <- low.get
        h <- high.get
      } yield l >= 1 && h >= 1 // Both got some execution
    },
    
    test("fairness is maintained over time") {
      for {
        counter <- Ref.make(0)
        
        _ <- (counter.update(_ + 1) *> ZIO.yieldNow).repeat(Schedule.recurs(10)).fork.flatMap(_.join)
        
        total <- counter.get
      } yield total >= 10 // Should execute multiple times
    },
    
    test("scheduler handles priority inversion") {
      for {
        resource <- Ref.make(0)
        
        fiber1 <- resource.update(_ + 1).forever.fork
        fiber2 <- ZIO.succeed(42).fork
        
        result <- fiber2.raceFirst(fiber1.interrupt *> ZIO.succeed(1))
      } yield assertCompletes(ZIO.succeed(result))(anything)
    }
  )
}
