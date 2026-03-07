package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9385
 * 
 * Issue: Error recovery strategies
 * 
 * This test verifies error recovery mechanisms.
 */
object BountySpec9385 extends ZIOSpecDefault {
  def spec = suite("Error recovery strategies")(
    test("ZIO.retry recovers from transient errors") {
      var attempts = 0
      
      val result = ZIO.attempt {
        attempts += 1
        if (attempts < 3) throw new Error("Temporary failure")
        else "success"
      }.retry(Schedule.recurs(5))
      
      assertCompletes(result)(equalTo("success"))
    },
    
    test("ZIO.catchAll recovers from errors") {
      val result = ZIO.fail(new Error("Failed"))
        .catchAll(_ => ZIO.succeed("recovered"))
      
      assertCompletes(result)(equalTo("recovered"))
    },
    
    test("ZIO.catchSome recovers from specific errors") {
      val result = ZIO.fail(new RuntimeException("Oops"))
        .catchSome { case _: RuntimeException =>
          ZIO.succeed("handled")
        }
      
      assertCompletes(result)(equalTo("handled"))
    },
    
    test("ZIO.retryOrElse retries then falls back") {
      var attempts = 0
      
      val result = ZIO.attempt {
        attempts += 1
        throw new Error("Always fails")
      }.retryOrElse(Schedule.recurs(3))(_ => ZIO.succeed("fallback"))
      
      assertCompletes(result)(equalTo("fallback"))
    },
    
    test("ZIO.eventually succeeds after retries") {
      var counter = 0
      
      val result = ZIO.attempt {
        counter += 1
        if (counter < 5) throw new Error("Not yet")
        else "done"
      }.eventually
      
      assertCompletes(result)(equalTo("done"))
    },
    
    test("error recovery preserves original cause") {
      val error = new Error("Original error")
      
      val result = ZIO.fail(error)
        .catchAll(e => ZIO.succeed(s"Caught: ${e.getMessage}"))
      
      assertCompletes(result)(equalTo("Caught: Original error"))
    }
  )
}
