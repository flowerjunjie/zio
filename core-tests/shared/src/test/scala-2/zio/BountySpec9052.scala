package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9052
 * 
 * Issue: ZStream scoped doesn't release the resource
 * 
 * This test verifies that scoped streams properly release resources.
 */
object BountySpec9052 extends ZIOSpecDefault {
  def spec = suite("ZStream scoped resource release")(
    test("ZStream.scoped releases resource after runDrain") {
      for {
        released <- Ref.make(false)
        
        stream = ZStream.scoped {
          ZIO.acquireRelease(
            ZIO.succeed("resource")
          )(_ => released.set(true))
        }.flatMap { _ =>
          ZStream(1, 2, 3)
        }
        
        _ <- stream.runDrain
        wasReleased <- released.get
      } yield wasReleased
    },
    
    test("ZStream.scoped releases on interruption") {
      for {
        released <- Ref.make(false)
        
        stream = ZStream.scoped {
          ZIO.acquireRelease(
            ZIO.succeed("resource")
          )(_ => released.set(true))
        }.flatMap { _ =>
          ZStream.never
        }
        
        fiber <- stream.runDrain.fork
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasReleased <- released.get
      } yield wasReleased
    },
    
    test("ZStream.scoped releases on error") {
      for {
        released <- Ref.make(false)
        
        stream = ZStream.scoped {
          ZIO.acquireRelease(
            ZIO.succeed("resource")
          )(_ => released.set(true))
        }.flatMap { _ =>
          ZStream.fail(new Error("Failed"))
        }
        
        result <- stream.runDrain.either
        wasReleased <- released.get
      } yield result.isLeft && wasReleased
    },
    
    test("ZStream.scoped releases for multiple scopes") {
      for {
        released1 <- Ref.make(false)
        released2 <- Ref.make(false)
        
        stream1 = ZStream.scoped(
          ZIO.acquireRelease(ZIO.succeed(1))(_ => released1.set(true))
        ).flatMap(_ => ZStream(1, 2))
        
        stream2 = ZStream.scoped(
          ZIO.acquireRelease(ZIO.succeed(2))(_ => released2.set(true))
        ).flatMap(_ => ZStream(3, 4))
        
        _ <- (stream1 ++ stream2).runDrain
        
        r1 <- released1.get
        r2 <- released2.get
      } yield r1 && r2
    }
  )
}
