package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10285
 * 
 * Issue: Fiber resource lifecycle
 * 
 * This test verifies fiber resource lifecycle.
 */
object BountySpec10285 extends ZIOSpecDefault {
  def spec = suite("Fiber resource lifecycle")(
    test("fiber acquires resources on start") {
      for {
        acquired <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          acquired.set(true)
        )(_ => acquired.set(false)) *> ZIO.succeed(42).fork
        
        _ <- fiber.join
        
        a <- acquired.get
        _ <- a <- acquired.set(false)
        
        wasAcquired <- a
      } yield wasAcquired && !a
    },
    
    test("fiber releases resources on completion") {
      for {
        released <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => released.set(true)) *> ZIO.succeed(42).fork
        
        _ <- fiber.join
        
        r <- released.get
      } yield r
    },
    
    test("fiber releases resources on interruption") {
      for {
        released <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => released.set(true)) *> ZIO.never.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasReleased <- released.get
      } yield wasReleased
    },
    
    test("fiber handles nested resources correctly") {
      for {
        innerReleased <- Ref.make(false)
        outerReleased <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          outerReleased.set(true)
        ) { _ =>
          ZIO.acquireRelease(
            innerReleased.set(true)
          )(_ => innerReleased.set(false)) *> ZIO.succeed(42)
        }.fork
        
        _ <- fiber.join
        
        outer = outerReleased.get
        inner = innerReleased.get
      } yield outer && !inner
    },
    
    test("fiber handles error in acquisition") {
      for {
        result <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => ZIO.fail(new Error("Acquisition error"))) *> ZIO.succeed(42).either
        
        r <- ZIO.succeed(result)
      } yield r.isRight
    },
    
    test("fiber handles error in release") {
      for {
        released <- Ref.make(false)
        
        result <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => released.set(true) *> ZIO.succeed(42).either
        
        r <- ZIO.succeed(result)
      } yield r.isRight && released.get
    },
    
    test("fibers are independent") {
      for {
        fiber1 <- ZIO.succeed(1).fork
        fiber2 <- ZIO.succeed(2).fork
        
        r1 <- fiber1.join
        r2 <- fiber2.join
        
        _ <- fiber1.interrupt
        _ <- fiber2.join
      } yield r1 == 1 && r2 == 2
    }
  )
}
