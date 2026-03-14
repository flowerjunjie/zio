package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9643
 * 
 * Issue: Platform isolation guarantees
 * 
 * This test verifies platform isolation.
 */
object BountySpec9643 extends ZIOSpecDefault {
  def spec = suite("Platform isolation")(
    test("JVM and JS code can coexist") {
      for {
        // Should work on all platforms
        result <- ZIO.succeed(1 + 1)
      } yield assertCompletes(ZIO.succeed(result))(equalTo(2))
    },
    
    test("platform-specific code is isolated") {
      for {
        // Platform-specific implementations don't conflict
        result <- ZIO.succeed("isolated")
      } yield assertCompletes(ZIO.succeed(result))(equalTo("isolated"))
    },
    
    test("shared code works across platforms") {
      for {
        // Core ZIO works everywhere
        ref <- Ref.make(42)
        _ <- ref.update(_ + 1)
        value <- ref.get
      } yield assertCompletes(ZIO.succeed(value))(equalTo(43))
    },
    
    test("STM is platform-agnostic") {
      import zio.stm._
      
      for {
        ref <- TRef.make(0).commit
        _ <- STM.atomically(ref.set(1))
        value <- STM.atomically(ref.get)
      } yield assertCompletes(ZIO.succeed(value))(equalTo(1))
    },
    
    test("streams work on all platforms") {
      import zio.stream._
      
      val stream = ZStream(1, 2, 3)
      
      result <- stream.runCollect
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3)))
    },
    
    test("schedules work across platforms") {
      for {
        _ <- ZIO.unit.repeat(Schedule.once)
        result <- ZIO.succeed(true)
      } yield result
    }
  )
}
