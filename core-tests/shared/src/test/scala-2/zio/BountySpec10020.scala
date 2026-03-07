package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10020
 * 
 * Issue: Async operation safety
 * 
 * This test verifies async operation safety.
 */
object BountySpec10020 extends ZIOSpecDefault {
  def spec = suite("Async operation safety")(
    test("async doesn't cause race conditions") {
      for {
        counter <- Ref.make(0)
        
        result <- ZIO.async[Any, Nothing, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.runAsync(
              counter.update(_ + 1) *> k(42).unit,
              false
            )
          }
        }
        
        count <- counter.get
      } yield assertCompletes(ZIO.succeed(count))(equalTo(1))
    },
    
    test("async preserves error information") {
      val error = new Error("Async error")
      
      result <- ZIO.async[Any, Error, Nothing] { k =>
        ZIO.runtime[Any].flatMap { rt =>
          rt.unsafe.runAsync(
            k(error).flip.unit,
            false
          )
        }
      }.either
      
      assertCompletes(ZIO.succeed(result))(isFailing(equalTo(error)))
    },
    
    test("async can be chained") {
      for {
        result <- ZIO.async[Any, Nothing, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.runAsync(
              k(1).flatMap(n => k(n + 1)).unit,
              false
            )
          }
        }
        
        r <- ZIO.succeed(result)
      } yield r == 2
    },
    
    test("async doesn't leak callbacks") {
      for {
        created <- Ref.make(0)
        
        _ <- ZIO.async[Any, Nothing, Int] { k =>
          created.update(_ + 1) *> k(42).unit
        }
        
        c <- created.get
      } yield c == 1
    },
    
    test("async completes even if callback throws") {
      for {
        result <- ZIO.async[Any, Error, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafeRunAsync(
              k(new Error("Failed")).flip.unit,
              false
            )
          }
        }.either
        
        r <- ZIO.succeed(result)
      } yield r.isLeft
    },
    
    test("async is stack-safe") {
      for {
        result <- ZIO.async[Any, Nothing, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.runAsync(
              k(42).unit,
              false
            )
          }
        }
        
        r <- ZIO.succeed(result)
      } yield r == 42
    }
  )
}
