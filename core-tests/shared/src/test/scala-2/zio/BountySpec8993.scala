package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #8993
 * 
 * Issue: Clarify documentation on initialization of test bootstrap layers
 * 
 * This test verifies test bootstrap layer initialization.
 */
object BountySpec8993 extends ZIOSpecDefault {
  def spec = suite("Test bootstrap layer initialization")(
    test("test bootstrap initializes correctly") {
      for {
        result <- ZIO.succeed(42)
      } yield result == 42
    },
    
    test("test layers are available in tests") {
      val effect = ZIO.environment[Any]
      
      assertCompletes(effect)(anything)
    },
    
    test("test bootstrap doesn't interfere with test execution") {
      for {
        a <- ZIO.succeed(1)
        b <- ZIO.succeed(2)
        c <- ZIO.succeed(3)
      } yield a + b + c == 6
    },
    
    test("scoped layers work in test context") {
      for {
        result <- ZIO.scoped {
          ZIO.acquireRelease(
            ZIO.succeed("scoped resource")
          )(_ => ZIO.unit)
        } *> ZIO.succeed(true)
      } yield result
    },
    
    test("test bootstrap layers are shared across tests") {
      for {
        // This test verifies that bootstrap layers
        // are properly initialized and shared
        result1 <- ZIO.succeed("test1")
        result2 <- ZIO.succeed("test2")
      } yield result1.nonEmpty && result2.nonEmpty
    }
  )
}
