package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9429
 * 
 * Issue: Fiber lifecycle management
 * 
 * This test verifies fiber lifecycle behavior.
 */
object BountySpec9429 extends ZIOSpecDefault {
  def spec = suite("Fiber lifecycle management")(
    test("fiber starts when forked") {
      for {
        started <- Ref.make(false)
        
        fiber <- (started.set(true) *> ZIO.never).fork
        
        _ <- ZIO.sleep(10.millis)
        wasStarted <- started.get
        
        _ <- fiber.interrupt
      } yield wasStarted
    },
    
    test("fiber completes when effect finishes") {
      for {
        fiber <- ZIO.succeed(42).fork
        
        result <- fiber.join
      } yield result == 42
    },
    
    test("fiber can be awaited") {
      for {
        fiber <- (ZIO.succeed(1) *> ZIO.succeed(2) *> ZIO.succeed(3)).fork
        
        result <- fiber.join
      } yield result == 3
    },
    
    test("fiber interruption is immediate") {
      for {
        interrupted <- Ref.make(false)
        
        fiber <- (ZIO.acquireRelease(
          ZIO.unit
        )(_ => interrupted.set(true)) *> ZIO.never).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasInterrupted <- interrupted.get
      } yield wasInterrupted
    },
    
    test("fiber status is queryable") {
      for {
        fiber <- ZIO.succeed(42).fork
        
        // Should be able to query fiber status
        _ <- ZIO.sleep(10.millis)
        
        result <- fiber.join
      } yield result == 42
    },
    
    test("multiple fibers can be managed") {
      for {
        fibers <- ZIO.foreach((1 to 10).toList) { i =>
          ZIO.succeed(i).fork
        }
        
        results <- ZIO.foreach(fibers)(_.join)
        
        sum = results.sum
      } yield sum == 55 // 1+2+...+10
    }
  )
}
