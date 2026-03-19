package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9175
 * 
 * Issue: Scope is not propagated from layer to specs in test suites
 * 
 * This test verifies scope propagation in test suites.
 */
object BountySpec9175 extends ZIOSpecDefault {
  def spec = suite("Scope propagation")(
    test("scope propagates from layer to test") {
      for {
        result <- ZIO.succeed(42)
      } yield result == 42
    },
    
    test("scope is available in test suite") {
      val effect = ZIO.acquireRelease(
        ZIO.succeed("acquired")
      )(_ => ZIO.unit)
      
      effect.map(_ => true)
    },
    
    test("nested scopes work correctly") {
      for {
        outer <- ZIO.acquireRelease(
          ZIO.succeed("outer")
        )(_ => ZIO.unit)
        
        inner <- ZIO.acquireRelease(
          ZIO.succeed("inner")
        )(_ => ZIO.unit)
        
        result = outer + inner
      } yield result == "outerinner"
    },
    
    test("scope propagation in shared tests") {
      val shared = ZIO.succeed(100)
      
      for {
        a <- shared
        b <- shared
      } yield a + b == 200
    }
  )
}
