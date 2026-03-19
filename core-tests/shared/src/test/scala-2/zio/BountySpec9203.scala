package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9203
 * 
 * Issue: ZIO.raceFirst sometimes prevents scope finalizers from running
 * 
 * This test verifies that raceFirst properly runs scope finalizers.
 */
object BountySpec9203 extends ZIOSpecDefault {
  def spec = suite("ZIO.raceFirst scope finalizers")(
    test("raceFirst runs finalizers for losing fiber") {
      for {
        finalized <- Ref.make(false)
        
        winner = ZIO.succeed(1)
        loser = ZIO.acquireRelease(
          finalized.set(true)
        )(_ => ZIO.unit) *> ZIO.never
        
        result <- winner.raceFirst(loser)
        wasFinalized <- finalized.get
      } yield result == 1 && wasFinalized
    },
    
    test("raceFirst runs all finalizers on success") {
      for {
        finalized1 <- Ref.make(false)
        finalized2 <- Ref.make(false)
        
        effect1 = ZIO.acquireRelease(
          finalized1.set(true)
        )(_ => ZIO.unit) *> ZIO.succeed(1)
        
        effect2 = ZIO.acquireRelease(
          finalized2.set(true)
        )(_ => ZIO.unit) *> ZIO.sleep(100.millis) *> ZIO.succeed(2)
        
        result <- effect1.raceFirst(effect2)
        f1 <- finalized1.get
        f2 <- finalized2.get
      } yield result == 1 && f1 && f2
    },
    
    test("raceFirst runs finalizers on interruption") {
      for {
        finalized <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          finalized.set(true)
        )(_ => ZIO.unit) *> ZIO.never.raceFirst(ZIO.succeed(1)).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasFinalized <- finalized.get
      } yield wasFinalized
    },
    
    test("raceFirst preserves scope for both effects") {
      for {
        counter <- Ref.make(0)
        
        effect1 = ZIO.acquireRelease(
          counter.update(_ + 1)
        )(_ => ZIO.unit) *> ZIO.succeed("a")
        
        effect2 = ZIO.acquireRelease(
          counter.update(_ + 1)
        )(_ => ZIO.unit) *> ZIO.sleep(10.millis) *> ZIO.succeed("b")
        
        result <- effect1.raceFirst(effect2)
        count <- counter.get
      } yield result == "a" && count == 2
    }
  )
}
