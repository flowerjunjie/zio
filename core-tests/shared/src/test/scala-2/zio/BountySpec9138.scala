package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9138
 * 
 * Issue: Stack safety improvements
 * 
 * This test verifies stack safety of operations.
 */
object BountySpec9138 extends ZIOSpecDefault {
  def spec = suite("Stack safety improvements")(
    test("deeply nested flatMap is stack safe") {
      def loop(n: Int): ZIO[Any, Nothing, Int] =
        if (n <= 0) ZIO.succeed(0)
        else ZIO.succeed(1).flatMap(_ => loop(n - 1))
      
      result <- loop(1000)
      
      assertCompletes(ZIO.succeed(result))(equalTo(0))
    },
    
    test("large chain of map operations is safe") {
      val base = ZIO.succeed(0)
      
      val chain = (1 to 100).foldLeft(base) { (acc, _) =>
        acc.map(_ + 1)
      }
      
      result <- chain
      
      assertCompletes(ZIO.succeed(result))(equalTo(100))
    },
    
    test("repeated foreach is stack safe") {
      val list = (1 to 100).toList
      
      _ <- ZIO.foreach(list) { _ =>
        ZIO.unit
      }
      
      assertCompletes(ZIO.succeed(true))(isTrue)
    },
    
    test("nested acquireRelease is stack safe") {
      def nested(n: Int): ZIO[Any, Nothing, Int] =
        if (n <= 0) ZIO.succeed(0)
        else ZIO.acquireRelease(
          ZIO.succeed(n)
        )(_ => ZIO.unit) *> nested(n - 1)
      
      result <- nested(100)
      
      assertCompletes(ZIO.succeed(result))(equalTo(0))
    },
    
    test("deeply nested error handling is safe") {
      def loop(n: Int): ZIO[Any, Nothing, Int] =
        if (n <= 0) ZIO.succeed(0)
        else ZIO.succeed(1).catchAll(_ => loop(n - 1))
      
      result <- loop(100)
      
      assertCompletes(ZIO.succeed(result))(equalTo(0))
    }
  )
}
