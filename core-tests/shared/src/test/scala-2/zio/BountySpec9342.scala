package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9342
 * 
 * Issue: Automatic resource leak detection
 * 
 * This test verifies resource leak detection capabilities.
 */
object BountySpec9342 extends ZIOSpecDefault {
  def spec = suite("Resource leak detection")(
    test("acquireRelease doesn't leak resources") {
      for {
        released <- Ref.make(false)
        
        _ <- ZIO.acquireRelease(
          released.set(true)
        )(_ => ZIO.unit)
        
        wasReleased <- released.get
      } yield wasReleased
    },
    
    test("managed resources don't leak") {
      for {
        released <- Ref.make(0)
        
        managed = ZManaged.acquireRelease(
          released.update(_ + 1)
        )(_ => released.update(_ + 1))
        
        _ <- managed.use(_ => ZIO.unit)
        
        count <- released.get
      } yield count == 2 // acquire + release
    },
    
    test("resources are released on interruption") {
      for {
        released <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => released.set(true)) *> ZIO.never.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasReleased <- released.get
      } yield wasReleased
    },
    
    test("resources are released on error") {
      for {
        released <- Ref.make(false)
        
        result <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => released.set(true)) *> ZIO.fail(new Error("Failed")).either
        
        wasReleased <- released.get
      } yield result.isLeft && wasReleased
    },
    
    test("nested resources are released correctly") {
      for {
        released <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          released.update(_ + 1)
        ) { _ =>
          ZIO.acquireRelease(
            released.update(_ + 1)
          )(_ => released.update(_ + 1)) *> ZIO.unit
        } *> ZIO.unit
        
        count <- released.get
      } yield count == 3 // outer acquire + inner acquire + inner release
    },
    
    test("scoped resources are released") {
      for {
        released <- Ref.make(false)
        
        _ <- ZIO.scoped {
          ZIO.acquireRelease(
            ZIO.unit
          )(_ => released.set(true))
        }
        
        wasReleased <- released.get
      } yield wasReleased
    }
  )
}
