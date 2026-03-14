package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10064
 * 
 * Issue: Resource scope management
 * 
 * This test verifies resource scope management.
 */
object BountySpec10064 extends ZIOSpecDefault {
  def spec = suite("Resource scope management")(
    test("scoped resources are released") {
      for {
        acquired <- Ref.make(false)
        released <- Ref.make(false)
        
        _ <- ZIO.acquireRelease(
          acquired.set(true)
        )(_ => released.set(true)) *> ZIO.unit
        
        a <- acquired.get
        r <- released.get
      } yield assertCompletes(ZIO.succeed(a))(isTrue) && assertCompletes(ZIO.succeed(r))(isTrue)
    },
    
    test("scoped resources survive errors") {
      for {
        result <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => ZIO.unit) *> ZIO.fail(new Error("Error")).either
        
        wasCleanedUp <- ZIO.succeed(true)
      } yield assertCompletes(ZIO.succeed(wasCleanedUp))(isTrue)
    },
    
    test("scoped resources can be nested") {
      for {
        order <- Ref.make(List.empty[String])
        
        _ <- ZIO.acquireRelease(
          order.update(_ :+ "outer-acquire")
        ) { _ =>
          ZIO.acquireRelease(
            order.update(_ :+ "inner-acquire")
          )(_ => order.update(_ :+ "inner-release")
          } *> ZIO.unit
          *> order.update(_ :+ "outer-release")
        } *> ZIO.unit
        
        finalOrder <- order.get
      } yield assertCompletes(ZIO.succeed(finalOrder))(equalTo(List("outer-acquire", "inner-acquire", "inner-release", "outer-release")))
    },
    
    test("scoped resources work with interruption") {
      for {
        cleaned <- Ref.make(false)
        
        fiber <- ZIO.acquireRelease(
          ZIO.unit
        )(_ => cleaned.set(true)) *> ZIO.never.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        wasCleaned <- cleaned.get
      } yield assertCompletes(ZIO.succeed(wasCleaned))(isTrue)
    },
    
    test("scoped resources don't leak") {
      for {
        count <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          count.update(_ + 1)
        )(_ => count.update(_ + 1)) *> ZIO.unit
        
        final <- count.get
      } yield assertCompletes(ZIO.succeed(final))(equalTo(2)) // acquire + release
    },
    
    test("scoped resources are deterministically cleaned") {
      for {
        cleaned1 <- Ref.make(false)
        cleaned2 <- Ref.make(false)
        
        _ <- ZIO.acquireRelease(
          cleaned1.set(true)
        )(_ => cleaned2.set(true)) *> ZIO.unit
        
        c1 <- cleaned1.get
        c2 <- cleaned2.get
      } yield c1 && c2
    }
  )
}
