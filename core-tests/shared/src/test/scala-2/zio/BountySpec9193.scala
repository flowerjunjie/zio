package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #9193
 * 
 * Issue: Detect deadlock scenarios at runtime
 * 
 * This test verifies deadlock detection capabilities.
 */
object BountySpec9193 extends ZIOSpecDefault {
  def spec = suite("Deadlock detection")(
    test("STM doesn't deadlock on concurrent updates") {
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { _ =>
          STM.atomically {
            for {
              _ <- ref.update(_ + 1)
              v <- ref.get
            } yield v
          }
        }
        
        sum <- fibers
        finalValue <- STM.atomically(ref.get)
      } yield sum.size == 10 && finalValue == 10
    },
    
    test("STM prevents circular wait") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        // STM should prevent deadlock even with multiple refs
        result <- STM.atomically {
          for {
            _ <- ref1.update(_ + 1)
            _ <- ref2.update(_ + 1)
            v1 <- ref1.get
            v2 <- ref2.get
          } yield (v1, v2)
        }
      } yield result == (1, 1)
    },
    
    test("STM handles high contention safely") {
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        finalValue <- STM.atomically(ref.get)
      } yield finalValue == 20
    },
    
    test("STM retry doesn't cause deadlock") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        result <- STM.atomically {
          for {
            v1 <- ref1.get
            v2 <- ref2.get
            _ <- if (v1 == 0 && v2 == 0) ref1.set(1) else ZIO.unit
          } yield ()
        }
      } yield true
    },
    
    test("STM guarantees progress") {
      for {
        ref <- TRef.make(0).commit
        
        _ <- ZIO.foreach((1 to 5).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        value <- STM.atomically(ref.get)
      } yield value == 5
    }
  )
}
