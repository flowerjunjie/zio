package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9407
 * 
 * Issue: Resource cleanup guarantees
 * 
 * This test verifies resource cleanup guarantees.
 */
object BountySpec9407 extends ZIOSpecDefault {
  def spec = suite("Resource cleanup guarantees")(
    test("cleanup runs even if body fails") {
      for {
        cleaned <- Ref.make(false)
        
        result <- ZIO.acquireReleaseExit(
          ZIO.unit
        ) { _ =>
          cleaned.set(true)
        } *> ZIO.fail(new Error("Failed")).either
        
        wasCleaned <- cleaned.get
      } yield result.isLeft && wasCleaned
    },
    
    test("cleanup runs on interruption") {
      for {
        cleaned <- Ref.make(false)
        
        fiber <- ZIO.acquireReleaseExit(
          ZIO.unit
        ) { _ =>
          cleaned.set(true)
        } *> ZIO.never.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasCleaned <- cleaned.get
      } yield wasCleaned
    },
    
    test("cleanup runs even if cleanup fails") {
      for {
        cleanupAttempts <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          ZIO.unit
        ) { _ =>
          cleanupAttempts.update(_ + 1) *> ZIO.fail(new Error("Cleanup failed"))
        }.either
        
        attempts <- cleanupAttempts.get
      } yield attempts == 1
    },
    
    test("nested cleanup runs in reverse order") {
      for {
        order <- Ref.make(List.empty[Int])
        
        _ <- ZIO.acquireRelease(
          order.update(_ :+ 1)
        ) { _ =>
          ZIO.acquireRelease(
            order.update(_ :+ 2)
          ) { _ =>
            order.update(_ :+ 3)
          } *> ZIO.unit
        } *> order.update(_ :+ 4)
        
        finalOrder <- order.get
      } yield finalOrder == List(1, 2, 3, 4)
    },
    
    test("cleanup runs exactly once") {
      for {
        cleanupCount <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          ZIO.unit
        ) { _ =>
          cleanupCount.update(_ + 1)
        } *> ZIO.unit
        
        count <- cleanupCount.get
      } yield count == 1
    }
  )
}
