package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #10307
 * 
 * Issue: STM transactions
 * 
 * This test verifies STM transaction behavior.
 */
object BountySpec10307 extends ZIOSpecDefault {
  def spec = suite("STM transactions")(
    test("transaction is atomic") {
      for {
        ref <- TRef.make(0).commit
        
        _ <- STM.atomically {
          for {
            _ <- ref.update(_ + 1)
            v <- ref.get
            _ <- ref.set(v + 1)
            _ <- ref.get
          } yield ()
        }
        
        result <- STM.atomically {
          for {
            _ <- ref.set(0)
            _ <- ref.get
          } yield ()
        }
        
        result <- STM.atomically(ref.get)
      } yield result == 0
    },
    
    test("transaction retry works") {
      for {
        ref <- TRef.make(false).commit
        
        _ <- STM.atomically {
          for {
            v <- ref.get
            _ <- if (!v) ref.set(true) else STM.retry
          } yield ()
        }
        
        result <- STM.atomically(ref.get)
      } yield result
    },
    
    test("transactions compose correctly") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        _ <- STM.atomically {
          for {
            _ <- ref1.set(1)
            _ <- ref2.set(2)
          } yield ()
        }
        
        r1 <- STM.atomically(ref1.get)
        r2 <- STM.atomically(ref2.get)
      } yield r1 == 1 && r2 == 2
    },
    
    test("concurrent transactions are isolated") {
      for {
        ref <- TRef.make(0).commit
        
        _ <- ZIO.foreachPar((1 to 10).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        result <- STM.atomically(ref.get)
      } yield result == 10
    },
    
    test("transactions are retryable") {
      for {
        ref <- TRef.make(0).commit
        
        _ <- ZIO.atomically {
          for {
            v <- ref.get
            _ <- if (v < 5) ref.set(v + 1) else STM.retry
          } yield ()
        }
        
        result <- STM.atomically(ref.get)
      } yield result == 5
    },
    
    test("transactions don't deadlock") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        _ <- ZIO.atomically {
          for {
            _ <- ref1.set(1)
            _ <- ref2.set(1)
            _ <- ref1.set(2)
          } yield ()
        }
        
        _ <- ZIO.atomically {
          for {
            _ <- ref1.get
            _ <- ref2.get
          } yield ()
        }
        
        r1 <- STM.atomically(ref1.get)
        r2 <- STM.atomically(ref2.get)
      } yield r1 == 2 && r2 == 2
    }
  )
}
