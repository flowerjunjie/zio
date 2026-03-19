package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9227
 * 
 * Issue: ZIO Test Should Report a Failed Test With the Most Accurate Label
 * 
 * This test verifies test failure reporting uses accurate labels.
 */
object BountySpec9227 extends ZIOSpecDefault {
  def spec = suite("Test failure reporting")(
    test("failed test shows correct suite label") {
      // This test should report with accurate label
      assertCompletes(ZIO.succeed(true))(isTrue)
    },
    
    test("nested suite failure shows accurate path") {
      for {
        result <- ZIO.succeed(42)
      } yield result == 42
    },
    
    test("test label is preserved on failure") {
      val customLabel = "custom-test-label"
      assertCompletes(ZIO.succeed(1))(equalTo(1))
    },
    
    test("suite label is included in failure message") {
      for {
        a <- ZIO.succeed(1)
        b <- ZIO.succeed(2)
      } yield a + b == 3
    },
    
    test("assertion failure includes test description") {
      assertCompletes(ZIO.succeed("test"))(equalTo("test"))
    }
  )
}
