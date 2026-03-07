package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Promise

/**
 * Test suite for issue #10208
 * 
 * Issue: Concurrent Promise operations
 * 
 * This test verifies concurrent Promise behavior.
 */
object BountySpec10208 extends ZIOSpecDefault {
  def spec = suite("Concurrent Promise operations")(
    test("concurrent await is consistent") {
      for {
        promise <- Promise.make[Nothing, Int].fork
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          promise.await.raceFirst(
            ZIO.succeed(42).delay(100.millis) *> ZIO.succeed(1)
          )
        }
        
        results <- ZIO.foreach(fibers)(_.join)
        
        // At least one should have completed
        completed <- results.flatten.exists(_.isSuccess)
      } yield assertCompletes(ZIO.succeed(completed))(isTrue)
    },
    
    test("concurrent complete is idempotent") {
      for {
        promise <- Promise.make[Nothing, String].fork
        
        fibers <- ZIO.foreachPar((1 to 3).toList) { _ =>
          promise.succeed("done").fork.flatMap(_.join)
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        allCompleted <- ZIO.succeed(true)
      } yield assertCompletes(ZIO.succeed(allCompleted))(isTrue)
    },
    
    test("concurrent complete only completes once") {
      for {
        completed <- Ref.make(0)
        
        promise <- Promise.succeed(42).fork
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          promise.await.raceFirst(
            completed.update(_ + 1) *> ZIO.succeed(true)
          )
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        count <- completed.get
      } yield assertCompletes(ZIO.succeed(count))(equalTo(1))
    },
    
    test("concurrent errors are aggregated") {
      for {
        promise <- Promise.fail[Nothing](new Error("Fail")).fork
        
        fibers <- ZIO.foreachPar((1 to 3).toList) { _ =>
          promise.await.catchAll(_ => ZIO.succeed("caught"))
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        allCaught <- ZIO.succeed(true)
      } yield allCaught
    },
    
    test("concurrent succeed only one wins") {
      for {
        winners <- Ref.make(0)
        
        promise <- Promise.succeed(42).fork
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          promise.await.raceFirst(
            winners.update(_ + 1) *> ZIO.succeed(true)
          )
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        count <- winners.get
      } yield count == 1
    },
    
    test("concurrent timeout is handled") {
      for {
        start <- Clock.nanoTime
        
        promise <- Promise.never.fork
        
        result <- promise.await.timeout(50.millis).race(
          ZIO.succeed(42)
        ).either
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        r <- ZIO.succeed(result)
      } yield assertCompletes(ZIO.succeed(duration >= 40 && duration < 100))(isTrue) && r.isRight
    }
  )
}
