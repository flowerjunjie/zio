package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.ZPool

/**
 * Test suite for issue #9152
 * 
 * Issue: ZPool leaks resources when acquisition is interrupted
 * 
 * This test verifies ZPool resource safety on interruption.
 */
object BountySpec9152 extends ZIOSpecDefault {
  def spec = suite("ZPool resource safety")(
    test("ZPool releases resources on interruption") {
      for {
        acquired <- Ref.make(0)
        released <- Ref.make(0)
        
        acquire = acquired.update(_ + 1)
        release = released.update(_ + 1)
        
        pool <- ZPool.make(acquire *> ZIO.succeed("resource"))(release)
        
        fiber <- pool.get.flatMap { _ =>
          ZIO.never // Hold the resource forever
        }.fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        a <- acquired.get
        r <- released.get
      } yield a == r && r >= 1
    },
    
    test("ZPool doesn't leak on concurrent interruption") {
      for {
        acquired <- Ref.make(0)
        released <- Ref.make(0)
        
        acquire = acquired.update(_ + 1)
        release = released.update(_ + 1)
        
        pool <- ZPool.make(acquire *> ZIO.succeed("resource"))(release)
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          pool.get.flatMap { _ =>
            ZIO.never
          }.fork
        }
        
        _ <- ZIO.sleep(50.millis)
        _ <- fibers.interrupt
        
        a <- acquired.get
        r <- released.get
      } yield a == r && r == 5
    },
    
    test("ZPool handles normal usage without leaks") {
      for {
        acquired <- Ref.make(0)
        released <- Ref.make(0)
        
        acquire = acquired.update(_ + 1)
        release = released.update(_ + 1)
        
        pool <- ZPool.make(acquire *> ZIO.succeed(1))(release)
        
        _ <- pool.get.use {_ =>
          ZIO.unit
        }
        
        a <- acquired.get
        r <- released.get
      } yield a == r && r >= 1
    },
    
    test("ZPool.release is called even if error during acquisition") {
      for {
        acquired <- Ref.make(0)
        released <- Ref.make(0)
        
        acquire = acquired.update(_ + 1) *> ZIO.fail(new Error("Fail"))
        release = released.update(_ + 1)
        
        result <- ZPool.make(acquire)(release).get.use(ZIO.succeed(_)).either
        
        a <- acquired.get
        r <- released.get
      } yield a == 1 && r == 1 && result.isLeft
    }
  )
}
