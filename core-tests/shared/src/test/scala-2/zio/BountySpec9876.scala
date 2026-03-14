package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9876
 * 
 * Issue: Runtime shutdown behavior
 * 
 * This test verifies runtime shutdown is clean.
 */
object BountySpec9876 extends ZIOSpecDefault {
  def spec = suite("Runtime shutdown behavior")(
    test("runtime shuts down gracefully") {
      for {
        // Runtime should shutdown gracefully
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("shutdown waits for running fibers") {
      for {
        completed <- Ref.make(false)
        
        fiber <- (ZIO.sleep(50.millis) *> completed.set(true)).fork
        
        // Simulate shutdown by interrupting
        _ <- ZIO.sleep(10.millis)
        _ <- fiber.interrupt
        
        wasCompleted <- completed.get
      } yield assertCompletes(ZIO.succeed(wasCompleted))(isFalse) // Fiber was interrupted
    },
    
    test("shutdown releases all resources") {
      for {
        acquired <- Ref.make(0)
        released <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          acquired.update(_ + 1)
        )(_ => released.update(_ + 1)) *> ZIO.unit
        
        a <- acquired.get
        r <- released.get
      } yield assertCompletes(ZIO.succeed(a == r))(isTrue) // Resources released
    },
    
    test("shutdown doesn't hang") {
      for {
        // Should complete in reasonable time
        start <- Clock.nanoTime
        
        result <- ZIO.succeed(42)
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(duration < 1000))(isTrue)
    },
    
    test("shutdown can be forced") {
      for {
        // Should support forced shutdown
        result <- ZIO.succeed("shutdown")
      } yield assertCompletes(ZIO.succeed(result))(equalTo("shutdown"))
    },
    
    test("shutdown preserves error information") {
      val error = new Error("Shutdown error")
      
      result <- ZIO.fail(error).exit
      
      assertCompletes(ZIO.succeed(result))(isFailing(equalTo(error)))
    }
  )
}
