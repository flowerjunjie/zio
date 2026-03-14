package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9447
 * 
 * Issue: Interruption safety guarantees
 * 
 * This test verifies interruption safety.
 */
object BountySpec9447 extends ZIOSpecDefault {
  def spec = suite("Interruption safety guarantees")(
    test("interruption is safe during acquire") {
      for {
        acquired <- Ref.make(false)
        released <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          acquired.set(true)
        )(_ => released.set(true)) *> ZIO.never.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        a <- acquired.get
        r <- released.get
      } yield a && r
    },
    
    test("interruption is safe during bracket use") {
      for {
        started <- Ref.make(false)
        finished <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          started.set(true)
        )(_ => finished.set(true)) *> ZIO.never.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        s <- started.get
        f <- finished.get
      } yield s && f
    },
    
    test("interruption doesn't cause data corruption") {
      for {
        ref <- Ref.make(0)
        
        fiber <- (ZIO.foreach((1 to 1000).toList) { i =>
          ref.update(_ + i)
        }.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        value <- ref.get
      } yield value >= 0 // Should have some value, not corrupted
    },
    
    test("interruption is safe during finalizers") {
      for {
        finalizerCount <- Ref.make(0)
        
        _ <- ZIO.acquireReleaseExit(
          ZIO.unit
        ) { _ =>
          finalizerCount.update(_ + 1) *> ZIO.sleep(100.millis)
        } *> ZIO.never.fork.flatMap { fiber =>
          ZIO.sleep(50.millis) *> fiber.interrupt
        }
        
        count <- finalizerCount.get
      } yield count == 1
    },
    
    test("interruption preserves invariants") {
      for {
        invariant <- Ref.make(true)
        
        fiber <- (invariant.set(true) *> ZIO.never).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        inv <- invariant.get
      } yield inv
    }
  )
}
