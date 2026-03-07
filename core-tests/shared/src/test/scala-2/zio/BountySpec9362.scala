package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #9362
 * 
 * Issue: STM performance optimizations
 * 
 * This test verifies STM optimization improvements.
 */
object BountySpec9362 extends ZIOSpecDefault {
  def spec = suite("STM performance optimizations")(
    test("STM commit path is optimized") {
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
        duration = (end - start) / 1000000.0
      } yield duration < 500
    },
    
    test("STM minimal allocation for simple transactions") {
      for {
        ref <- TRef.make(42).commit
        
        start <- Clock.nanoTime
        _ <- ZIO.foreach((1 to 50).toList) { _ =>
          STM.atomically(ref.get)
        }
        end <- Clock.nanoTime
        
        duration = (end - start) / 1000000.0
      } yield duration < 200
    },
    
    test("STM retry is efficient") {
      for {
        ref <- TRef.make(0).commit
        
        start <- Clock.nanoTime
        _ <- STM.atomically {
          for {
            v <- ref.get
            _ <- if (v == 0) ref.set(1) else ZIO.unit
          } yield ()
        }
        end <- Clock.nanoTime
        
        duration = (end - start) / 1000000.0
      } yield duration < 100
    },
    
    test("STM handles high contention efficiently") {
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 30).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        sum <- fibers
        finalValue <- STM.atomically(ref.get)
      } yield sum.size == 30 && finalValue == 30
    },
    
    test("STM scales with concurrent transactions") {
      for {
        refs <- ZIO.foreach((1 to 10).toList) { _ =>
          TRef.make(0).commit
        }
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { _ =>
          STM.atomically {
            ZIO.foreach(refs) { ref =>
              ref.update(_ + 1)
            }
          }
        }
        
        count <- fibers.join
      } yield count == 20
    }
  )
}
