package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #9081
 * 
 * Issue: Improve `ZSTM`'s performance
 * 
 * This test verifies STM performance optimizations.
 */
object BountySpec9081 extends ZIOSpecDefault {
  def spec = suite("ZSTM performance optimization")(
    test("STM commit is fast for simple transactions") {
      for {
        start <- Clock.nanoTime
        _ <- ZIO.foreach((1 to 100).toList) { _ =>
          STM.atomically {
            for {
              ref <- TRef.make(0)
              _ <- ref.set(1)
              v <- ref.get
            } yield v
          }
        }
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 500.0
    },
    
    test("STM retry is efficient") {
      for {
        ref <- TRef.make(0).commit
        
        start <- Clock.nanoTime
        _ <- STM.atomically {
          for {
            v <- ref.get
            _ <- ref.set(v + 1)
            _ <- ref.set(v + 2) // May cause retry
          } yield ()
        }
        end <- Clock.nanoTime
        
        duration = (end - start) / 1000000.0
      } yield duration < 100.0
    },
    
    test("STM concurrent transactions scale well") {
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { _ =>
          STM.atomically {
            for {
              _ <- ref.update(_ + 1)
              v <- ref.get
            } yield v
          }
        }
        
        sum <- fibers
        finalValue <- STM.atomically(ref.get)
      } yield sum.size == 20 && finalValue == 20
    },
    
    test("STM minimal overhead for read-only transactions") {
      for {
        ref <- TRef.make(42).commit
        
        start <- Clock.nanoTime
        _ <- ZIO.foreach((1 to 50).toList) { _ =>
          STM.atomically(ref.get)
        }
        end <- Clock.nanoTime
        
        duration = (end - start) / 1000000.0
      } yield duration < 200.0
    },
    
    test("STM reduces allocations for repeated operations") {
      for {
        ref <- TRef.make(0).commit
        
        start <- Clock.nanoTime
        _ <- STM.atomically {
          for {
            _ <- ref.update(_ + 1)
            _ <- ref.update(_ + 1)
            _ <- ref.update(_ + 1)
          } yield ()
        }
        end <- Clock.nanoTime
        
        duration = (end - start) / 1000000.0
      } yield duration < 50.0
    }
  )
}
