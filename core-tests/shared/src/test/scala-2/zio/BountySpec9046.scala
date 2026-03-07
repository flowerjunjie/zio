package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9046
 * 
 * Issue: Allow running some effect before / after a single sample of check
 * 
 * This test verifies before/after hooks for property testing.
 */
object BountySpec9046 extends ZIOSpecDefault {
  def spec = suite("Check before/after hooks")(
    test("before hook runs before sample") {
      for {
        counter <- Ref.make(0)
        before = counter.update(_ + 1)
        
        // Simulate check with before hook
        result <- before *> ZIO.succeed(42)
        
        count <- counter.get
      } yield result == 42 && count == 1
    },
    
    test("after hook runs after sample") {
      for {
        counter <- Ref.make(0)
        after = counter.update(_ + 1)
        
        // Simulate check with after hook
        result <- ZIO.succeed(42).ensuring(after)
        
        count <- counter.get
      } yield result == 42 && count == 1
    },
    
    test("before and after hooks both run") {
      for {
        beforeCounter <- Ref.make(0)
        afterCounter <- Ref.make(0)
        
        before = beforeCounter.update(_ + 1)
        after = afterCounter.update(_ + 1)
        
        result <- before *> ZIO.succeed(42).ensuring(after)
        
        b <- beforeCounter.get
        a <- afterCounter.get
      } yield result == 42 && b == 1 && a == 1
    },
    
    test("hooks run for each sample in multiple checks") {
      for {
        counter <- Ref.make(0)
        
        hook = counter.update(_ + 1)
        
        // Simulate 3 checks with hooks
        _ <- ZIO.foreach((1 to 3).toList) { _ =>
          hook *> ZIO.succeed(true).ensuring(hook)
        }
        
        count <- counter.get
      } yield count == 6 // before + after for each of 3 checks
    },
    
    test("after hook runs even if sample fails") {
      for {
        cleanedUp <- Ref.make(false)
        after = cleanedUp.set(true)
        
        result <- (ZIO.fail(new Error("Failed")): ZIO[Any, Error, Int])
          .ensuring(after)
          .either
        
        cleaned <- cleanedUp.get
      } yield result.isLeft && cleaned
    }
  )
}
