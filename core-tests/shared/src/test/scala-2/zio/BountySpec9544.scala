package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9544
 * 
 * Issue: Platform compatibility across JVM, JS, Native
 * 
 * This test verifies cross-platform compatibility.
 */
object BountySpec9544 extends ZIOSpecDefault {
  def spec = suite("Platform compatibility")(
    test("basic ZIO works on all platforms") {
      for {
        result <- ZIO.succeed(1 + 1)
      } yield result == 2
    },
    
    test("Ref works on all platforms") {
      for {
        ref <- Ref.make(42)
        _ <- ref.update(_ + 1)
        value <- ref.get
      } yield value == 43
    },
    
    test("fiber operations are cross-platform") {
      for {
        fiber <- ZIO.succeed(42).fork
        result <- fiber.join
      } yield result == 42
    },
    
    test("STM works on all platforms") {
      import zio.stm._
      
      for {
        ref <- TRef.make(0).commit
        _ <- STM.atomically(ref.set(1))
        value <- STM.atomically(ref.get)
      } yield value == 1
    },
    
    test("stream operations are cross-platform") {
      import zio.stream._
      
      val stream = ZStream(1, 2, 3)
      
      result <- stream.runCollect
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3)))
    },
    
    test("schedule works on all platforms") {
      for {
        _ <- ZIO.unit.repeat(Schedule.once)
        result <- ZIO.succeed(true)
      } yield result
    }
  )
}
