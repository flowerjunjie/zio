package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9015
 * 
 * Issue: ZIO#cachedInvalidate blocks cancelation on reload after invalidate
 * 
 * This test verifies cachedInvalidate respects cancellation.
 */
object BountySpec9015 extends ZIOSpecDefault {
  def spec = suite("ZIO.cachedInvalidate cancellation")(
    test("cachedInvalidate respects cancellation") {
      for {
        invalidated <- Ref.make(false)
        
        effect = ZIO.succeed(42).onCancel(invalidated.set(true))
        
        cached <- effect.cachedInvalidate
        
        fiber <- cached.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasInvalidated <- invalidated.get
      } yield wasInvalidated
    },
    
    test("cachedInvalidate allows reload after invalidate") {
      for {
        counter <- Ref.make(0)
        
        effect = counter.updateAndGet(_ + 1)
        
        cached <- effect.cachedInvalidate
        
        value1 <- cached
        _ <- cached.invalidate
        value2 <- cached
        
        c <- counter.get
      } yield value1 == 1 && value2 == 2 && c == 2
    },
    
    test("cachedInvalidate doesn't block on concurrent invalidation") {
      for {
        cached <- ZIO.succeed(42).cachedInvalidate
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          cached.invalidate *> cached
        }
        
        results <- fibers
      } yield results.forall(_ == 42)
    },
    
    test("cachedInvalidate cleanup happens on invalidation") {
      for {
        cleaned <- Ref.make(false)
        
        effect = ZIO.acquireRelease(
          ZIO.succeed("resource")
        )(_ => cleaned.set(true)) *> ZIO.succeed(42)
        
        cached <- effect.cachedInvalidate
        
        _ <- cached
        _ <- cached.invalidate
        
        wasCleaned <- cleaned.get
      } yield wasCleaned
    }
  )
}
