package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10263
 * 
 * Issue: Async error handling
 * 
 * This test verifies async error behavior.
 */
object BountySpec10263 extends ZIOSpecDefault {
  def spec = suite("Async error handling")(
    test("async errors are recoverable") {
      val error = new Error("Async error")
      
      result <- ZIO.async[Any, Error, Int] { k =>
        ZIO.runtime[Any].flatMap { rt =>
          rt.unsafe.runAsync(
            k(error).flip.unit,
            false
          )
        }
      }.catchAll(_ => ZIO.succeed(0))
      
      r <- ZIO.succeed(result)
    } yield r == 0
    },
    
    test("async errors don't cause crashes") {
      for {
        result <- ZIO.async[Any, Error, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafeRunAsync(
              k(new Error("Fail")).flip.unit,
              false
            )
          )
        }.catchAll(_ => ZIO.succeed(0))
        
        _ <- ZIO.yieldNow
        
        r <- ZIO.succeed(result)
      } yield r == 0
    },
    
    test("async preserves error information") {
      val error = new Error("Detailed async error")
      
      result <- ZIO.async[Any, Error, String] { k =>
        ZIO.runtime[Any].flatMap { rt =>
          rt.unsafeRunAsync(
            k(error).map(msg => s"Caught: $msg").flip.unit,
            false
          )
        }
      }.either
      
      msg <- result.swap.getOrElse("No error")
      
      r <- ZIO.succeed(msg)
    } yield assertCompletes(ZIO.succeed(r))(contains("Detailed"))
    },
    
    test("async is thread-safe") {
      for {
        results <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ZIO.async[Any, Nothing, Int] { k =>
            ZIO.runtime[Any].flatMap { rt =>
              rt.unsafeRunAsync(
                k(42).unit,
                false
              )
            }
          }
        }
        
        allSucceeded <- ZIO.foreach(results)(_.exit)
        
        all <- ZIO.foreach(allSucceeded)(_.isSuccess)
      } yield all
    },
    
    test("async completion is immediate") {
      for {
        start <- Clock.nanoTime
        
        result <- ZIO.async[Any, Nothing, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafeRunAsync(
              k(42).unit,
              false
            )
          )
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        r <- ZIO.succeed(result == 42 && duration < 100)
      } yield r
    }
  )
}
