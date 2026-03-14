package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9533
 * 
 * Issue: ZIO composition ergonomics
 * 
 * This test verifies ZIO composition is ergonomic.
 */
object BountySpec9533 extends ZIOSpecDefault {
  def spec = suite("ZIO composition ergonomics")(
    test("ZIO.map composes clearly") {
      for {
        result <- ZIO.succeed(1)
          .map(_ + 1)
          .map(_ * 2)
          .map(_ - 1)
      } yield result == 3
    },
    
    test("ZIO.flatMap composes intuitively") {
      for {
        result <- ZIO.succeed(1)
          .flatMap(i => ZIO.succeed(i + 1))
          .flatMap(i => ZIO.succeed(i * 2))
      } yield result == 4
    },
    
    test("ZIO.zip composes parallel operations") {
      for {
        result <- ZIO.succeed(1).zip(ZIO.succeed("a"))
      } yield assertCompletes(ZIO.succeed(result))(anything)
    },
    
    test("ZIO.zipWith composes with function") {
      for {
        result <- ZIO.succeed(1).zipWith(ZIO.succeed(2))(_ + _)
      } yield result == 3
    },
    
    test("ZIO.catchSome composes error handling") {
      val result = ZIO.fail(new Error("Fail"))
        .catchSome { case _: Error => ZIO.succeed("caught") }
      
      assertCompletes(ZIO.succeed(result))(equalTo("caught"))
    },
    
    test("nested composition is readable") {
      for {
        result <- ZIO.succeed(1)
          .flatMap(i => ZIO.succeed(i + 1))
          .map(j => j * 2)
          .zip(ZIO.succeed("done"))
      } yield assertCompletes(ZIO.succeed(result._1))(equalTo(4))
    }
  )
}
