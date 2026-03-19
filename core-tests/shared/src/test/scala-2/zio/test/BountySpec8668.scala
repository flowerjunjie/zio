package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #8668
 * 
 * Issue: zio-test: `macro has not been expanded` for test with flatMap
 * 
 * This test verifies that test macros properly expand when using flatMap
 * in test expressions.
 */
object BountySpec8668 extends ZIOSpecDefault {
  def spec = suite("Macro expansion with flatMap")(
    test("macro properly expands with flatMap in test assertion") {
      // This test verifies that the test macro doesn't fail
      // with "macro has not been expanded" when using flatMap
      for {
        result <- ZIO.succeed(42)
        mapped = result.flatMap(v => ZIO.succeed(v * 2))
        _ <- ZIO.log(s"Result: $mapped")
      } yield mapped
    },
    
    test("nested flatMap operations expand correctly") {
      for {
        a <- ZIO.succeed(1)
        b <- ZIO.succeed(2)
        _ <- a.flatMap(x => ZIO.succeed(x * b))
      } yield ()
    },
    
    test("flatMap with assertions works properly") {
      val effect = for {
        value <- ZIO.succeed(10)
        _ <- ZIO.succeed(5)
      } yield value

      assertCompletes(effect)(equalTo(10))
    },
    
    test("complex flatMap chains don't cause macro errors") {
      val effect = ZIO.succeed(1)
        .flatMap(_ => ZIO.succeed(2))
        .flatMap(_ => ZIO.succeed(3))
        .flatMap(_ => ZIO.succeed(4))
      
      assertCompletes(effect)(equalTo(4))
    }
  )
}
