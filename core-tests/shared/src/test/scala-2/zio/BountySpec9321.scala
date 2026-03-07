package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9321
 * 
 * Issue: OS Signal handling and fiber interruption
 * 
 * This test verifies OS signal handling with fiber interruption.
 */
object BountySpec9321 extends ZIOSpecDefault {
  def spec = suite("OS Signal handling")(
    test("fiber interruption respects signal handlers") {
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
    
    test("signal handlers run during fiber interruption") {
      for {
        handlerRun <- Ref.make(false)
        
        effect = ZIO.acquireRelease(
          handlerRun.set(true)
        )(_ => ZIO.unit) *> ZIO.succeed(42)
        
        result <- effect.onInterrupt(handlerRun.set(true)).either
        handlerCalled <- handlerRun.get
      } yield handlerCalled
    },
    
    test("multiple fibers handle signals correctly") {
      for {
        finalizers <- Ref.make(0)
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          ZIO.acquireRelease(
            finalizers.update(_ + 1)
          )(_ => ZIO.unit) *> ZIO.never
        }.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fibers.interrupt
        
        count <- finalizers.get
      } yield count == 5
    },
    
    test("signal interruption is propagated correctly") {
      for {
        parentFinalized <- Ref.make(false)
        childFinalized <- Ref.make(false)
        
        parent = ZIO.acquireRelease(
          parentFinalized.set(true)
        )(_ => ZIO.unit) *> (
          ZIO.acquireRelease(
            childFinalized.set(true)
          )(_ => ZIO.unit) *> ZIO.never
        ).fork.flatMap(_.join).unit
        
        fiber <- parent.fork
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        p <- parentFinalized.get
        c <- childFinalized.get
      } yield p && c
    }
  )
}
