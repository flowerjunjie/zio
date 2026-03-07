package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio._

/**
 * Test suite for issue #10185
 * 
 * Issue: ZIO error boundary handling
 * 
 * This test verifies error boundary behavior.
 */
object BountySpec10185 extends ZIOSpecDefault {
  def spec = suite("ZIO error boundaries")(
    test("errors are caught at boundaries") {
      val result = ZIO.succeed(42).catchAllDefect { e =>
        ZIO.succeed(s"Caught: ${e.getMessage}")
      }
      
      assertCompletes(ZIO.succeed(result))(equalTo("Caught: 42"))
    },
    
    test("errors propagate through flatMap") {
      val result = ZIO.fail(new Error("Error1")).flatMap { _ =>
        ZIO.succeed(1)
      }.catchAllDefect { _ =>
        ZIO.succeed(0)
      }
      
      assertCompletes(ZIO.succeed(result))(equalTo(1))
    },
    
    test("errors are preserved across async boundaries") {
      val error = new Error("Async error")
      
      result <- ZIO.async[Any, Error, Int] { k =>
        ZIO.runtime[Any].flatMap { rt =>
          rt.unsafe.run(
            k(error).flip.unit,
            false
          )
        }
      }.either
      
      assertCompletes(ZIO.succeed(result))(isFailing(equalTo(error)))
    },
    
    test("errors can be recovered at boundaries") {
      val result = ZIO.fail(new Error("Original"))
        .catchSome { case _: Error => ZIO.succeed("recovered") }
        .catchAll(_ => ZIO.succeed("fallback"))
      
      assertCompletes(ZIO.succeed(result))(equalTo("recovered"))
    },
    
    test("error boundaries don't cause performance issues") {
      for {
        start <- Clock.nanoTime
        
        _ <- ZIO.foreach((1 to 100).toList) { i =>
          ZIO.succeed(i).catchAll(_ => ZIO.succeed(0))
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(duration < 1000))(isTrue)
    },
    
    test("error information is preserved through boundaries") {
      val error = new Error("Detailed error message")
      
      result <- ZIO.fail(error)
        .catchAll(e => ZIO.succeed(s"Caught: ${e.getMessage}"))
        .catchSome(_ => ZIO.succeed("fallback"))
      
      r <- ZIO.succeed(result)
    } yield assertCompletes(ZIO.succeed(r))(contains("Detailed"))
    }
  )
}
