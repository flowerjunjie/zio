package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9396
 * 
 * Issue: Async boundary behavior
 * 
 * This test verifies async boundary handling.
 */
object BountySpec9396 extends ZIOSpecDefault {
  def spec = suite("Async boundary behavior")(
    test("async executes effect asynchronously") {
      for {
        ref <- Ref.make(false)
        
        _ <- ZIO.async[Any, Nothing, Unit] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(
              ref.set(true) *> k(()).unit
            )
          }
        }
        
        result <- ref.get
      } yield result
    },
    
    test("async doesn't block current fiber") {
      for {
        start <- Clock.nanoTime
        
        _ <- ZIO.async[Any, Nothing, Unit] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(
              ZIO.sleep(50.millis) *> k(()).unit
            )
          }
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 20 // Should not block
    },
    
    test("async preserves error information") {
      val error = new Error("Async error")
      
      result <- ZIO.async[Any, Error, Nothing] { k =>
        ZIO.runtime[Any].flatMap { rt =>
          rt.unsafe.run(
            ZIO.fail(error).flatMap(k).flip
          )
        }
      }.either
      
      assertCompletes(ZIO.succeed(result))(isFailing(equalTo(error)))
    },
    
    test("async can be chained") {
      for {
        result <- ZIO.async[Any, Nothing, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(
              ZIO.succeed(42).flatMap(k).unit
            )
          }
        }.map(_ * 2)
      } yield result == 84
    },
    
    test("async works with multiple async boundaries") {
      for {
        result <- ZIO.async[Any, Nothing, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(
              ZIO.async[Any, Nothing, Int] { k2 =>
                ZIO.runtime[Any].flatMap { rt2 =>
                  rt2.unsafe.run(
                    ZIO.succeed(42).flatMap(k2).unit
                  )
                }
              }.flatMap(k).unit
            )
          }
        }
      } yield result == 42
    }
  )
}
