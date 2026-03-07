package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9303
 * 
 * Issue: Interruption propagation in raceFirst
 * 
 * This test verifies raceFirst interruption behavior.
 */
object BountySpec9303 extends ZIOSpecDefault {
  def spec = suite("raceFirst interruption propagation")(
    test("raceFirst interrupts loser on winner completion") {
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
    
    test("raceFirst propagates interruption from winner") {
      for {
        interrupted <- Ref.make(false)
        
        winner = ZIO.acquireRelease(
          ZIO.unit
        )(_ => interrupted.set(true)) *> ZIO.never
        
        loser = ZIO.succeed(2)
        
        fiber <- winner.raceFirst(loser).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasInterrupted <- interrupted.get
      } yield wasInterrupted
    },
    
    test("raceFirst with both effects interruptible") {
      for {
        interrupted1 <- Ref.make(false)
        interrupted2 <- Ref.make(false)
        
        effect1 = ZIO.acquireRelease(
          ZIO.unit
        )(_ => interrupted1.set(true)) *> ZIO.succeed(1)
        
        effect2 = ZIO.acquireRelease(
          ZIO.unit
        )(_ => interrupted2.set(true)) *> ZIO.never
        
        result <- effect1.raceFirst(effect2)
        
        i1 <- interrupted1.get
        i2 <- interrupted2.get
      } yield result == 1 && i2
    },
    
    test("raceFirst interruption is immediate") {
      for {
        start <- Clock.nanoTime
        
        result <- ZIO.succeed(1).raceFirst(ZIO.never)
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield result == 1 && duration < 100
    },
    
    test("raceFirst handles nested interruptions") {
      for {
        innerInterrupted <- Ref.make(false)
        outerInterrupted <- Ref.make(false)
        
        inner = ZIO.acquireRelease(
          ZIO.unit
        )(_ => innerInterrupted.set(true)) *> ZIO.never
        
        outer = inner.raceFirst(ZIO.succeed(1))
        
        fiber <- outer.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        innerI <- innerInterrupted.get
      } yield innerI
    }
  )
}
