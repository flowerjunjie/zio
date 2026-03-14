package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9141
 * 
 * Issue: Add pairwise `raceFirst` operator
 * 
 * This test verifies pairwise raceFirst functionality.
 */
object BountySpec9141 extends ZIOSpecDefault {
  def spec = suite("raceFirst pairwise operator")(
    test("raceFirst with 2 effects works") {
      for {
        result <- ZIO.succeed(1).raceFirst(ZIO.succeed(2))
      } yield result == 1 || result == 2
    },
    
    test("raceFirst returns first to complete") {
      for {
        result <- ZIO.succeed(1).raceFirst(
          ZIO.sleep(100.millis) *> ZIO.succeed(2)
        )
      } yield result == 1
    },
    
    test("raceFirst with 3 effects (pairwise)") {
      for {
        result <- (
          ZIO.succeed(1).raceFirst(ZIO.succeed(2))
        ).raceFirst(
          ZIO.succeed(3)
        )
      } yield result >= 1 && result <= 3
    },
    
    test("raceFirst interrupts loser") {
      for {
        interrupted <- Ref.make(false)
        
        winner = ZIO.succeed(1)
        loser = ZIO.acquireRelease(
          ZIO.unit
        )(_ => interrupted.set(true)) *> ZIO.never
        
        result <- winner.raceFirst(loser)
        wasInterrupted <- interrupted.get
      } yield result == 1 && wasInterrupted
    },
    
    test("raceFirst propagates errors") {
      val result = ZIO.fail(new Error("Failed"))
        .raceFirst(ZIO.sleep(100.millis) *> ZIO.succeed(1))
        .exit
      
      assertCompletes(result)(isFailing(anything))
    }
  )
}
