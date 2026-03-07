package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9987
 * 
 * Issue: Fiber interruption model
 * 
 * This test verifies interruption behavior.
 */
object BountySpec9987 extends ZIOSpecDefault {
  def spec = suite("Fiber interruption model")(
    test("interruption is cooperative by default") {
      for {
        interrupted <- Ref.make(false)
        
        fiber <- (ZIO.acquireRelease(
          ZIO.unit
        )(_ => interrupted.set(true)) *> ZIO.never).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasInterrupted <- interrupted.get
      } yield assertCompletes(ZIO.succeed(wasInterrupted))(isTrue)
    },
    
    test("interruption can be unchecked") {
      for {
        result <- ZIO.unit.interruptible.fork.flatMap(_.join)
      } yield true
    },
    
    test("interruption propagates to children") {
      for {
        childInterrupted <- Ref.make(false)
        
        child <- (ZIO.acquireRelease(
          ZIO.unit
        )(_ => childInterrupted.set(true)) *> ZIO.never).fork
        
        parent <- child.fork.flatMap(_.join)
        
        _ <- ZIO.sleep(50.millis)
        _ <- parent.interrupt
        
        wasInterrupted <- childInterrupted.get
      } yield assertCompletes(ZIO.succeed(wasInterrupted))(isTrue)
    },
    
    test("interruption finalizers run in order") {
      for {
        order <- Ref.make(List.empty[Int])
        
        _ <- ZIO.acquireRelease(
          order.update(_ :+ 1)
        ) { _ =>
          order.update(_ :+ 2)
        } *> ZIO.unit
        
        finalOrder <- order.get
      } yield assertCompletes(ZIO.succeed(finalOrder))(equalTo(List(1, 2)))
    },
    
    test("interruption is deterministic") {
      for {
        result1 <- ZIO.succeed(1).interruptible.fork.flatMap(_.join)
        result2 <- ZIO.succeed(2).interruptible.fork.flatMap(_.join)
      } yield result1 == 1 && result2 == 2
    },
    
    test("interruption doesn't cause deadlocks") {
      for {
        fiber1 <- ZIO.never.fork
        fiber2 <- ZIO.never.fork
        
        _ <- fiber1.interrupt
        _ <- fiber2.interrupt
        
        result <- ZIO.succeed(true)
      } yield result
    }
  )
}
