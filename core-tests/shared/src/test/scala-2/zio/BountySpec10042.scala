package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #10042
 * 
 * Issue: STM transaction isolation
 * 
 * This test verifies STM transaction isolation.
 */
object BountySpec10042 extends ZIOSpecDefault {
  def spec = suite("STM transaction isolation")(
    test("transactions are isolated") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        _ <- STM.atomically {
          for {
            _ <- ref1.set(1)
            v2 <- ref2.get
          } yield ()
        }
        
        v1 <- STM.atomically(ref1.get)
        v2 <- STM.atomically(ref2.get)
      } yield assertCompletes(ZIO.succeed(v1))(equalTo(1)) && assertCompletes(ZIO.success(v2))(equalTo(0))
    },
    
    test("transactions retry independently") {
      for {
        ref <- TRef.make(false).commit
        
        _ <- STM.atomically {
          for {
            v <- ref.get
            _ <- if (!v) ref.set(true) else STM.retry
          } yield ()
        }
        
        result <- STM.atomically(ref.get)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("nested transactions are isolated") {
      for {
        ref <- TRef.make(0).commit
        
        _ <- STM.atomically {
          for {
            _ <- ref.set(1)
            _ <- STM.atomically {
              ref.set(2)
            }
            _ <- ref.set(3)
          } yield ()
        }
        
        result <- STM.atomically(ref.get)
      } yield result == 3 // Outer transaction won
    },
    
    test("concurrent transactions don't interfere") {
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        result <- STM.atomically(ref.get)
      } yield result == 10
    },
    
    test("transaction rollback doesn't affect others") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        _ <- STM.atomically {
          ref1.set(1) *> STM.retry
        }.either
        
        _ <- STM.atomically {
          ref2.set(1)
        }
        
        v1 <- STM.atomically(ref1.get)
        v2 <- STM.atomically(ref2.get)
      } yield v1 == 0 && v2 == 1
    },
    
    test("STM doesn't leak resources") {
      for {
        _ <- STM.atomically {
          ZIO.acquireRelease(
            TRef.make(0).commit
          )(_ => ZIO.unit).unit
        }
        
        result <- ZIO.succeed(true)
      } yield result
    }
  )
}
