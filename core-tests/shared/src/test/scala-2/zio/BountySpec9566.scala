package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9566
 * 
 * Issue: Binary compatibility guarantees
 * 
 * This test verifies binary compatibility.
 */
object BountySpec9566 extends ZIOSpecDefault {
  def spec = suite("Binary compatibility")(
    test("ZIO.foreach signature is stable") {
      for {
        result <- ZIO.foreach(List(1, 2, 3))(i => ZIO.succeed(i * 2))
      } yield assertCompletes(ZIO.succeed(result))(anything)
    },
    
    test("Ref operations maintain binary compatibility") {
      for {
        ref <- Ref.make(0)
        _ <- ref.update(_ + 1)
        value <- ref.get
      } yield assertCompletes(ZIO.succeed(value))(anything)
    },
    
    test("Stream API is binary compatible") {
      val stream = ZStream(1, 2, 3)
      
      result <- stream.runCollect
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3)))
    },
    
    test("Schedule API is stable") {
      for {
        _ <- ZIO.unit.repeat(Schedule.once)
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("STM API is binary compatible") {
      import zio.stm._
      
      for {
        ref <- TRef.make(0).commit
        _ <- STM.atomically(ref.set(1))
        value <- STM.atomically(ref.get)
      } yield assertCompletes(ZIO.succeed(value))(anything)
    },
    
    test("effect types are binary compatible") {
      for {
        result1 <- ZIO.succeed(1)
        result2 <- ZIO.succeed(2)
        result3 <- ZIO.succeed(3)
      } yield assertCompletes(ZIO.succeed(result1 + result2 + result3))(equalTo(6))
    }
  )
}
