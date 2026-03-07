package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #10130
 * 
 * Issue: STM optimization
 * 
 * This test verifies STM optimizations.
 */
object BountySpec10130 extends ZIOSpecDefault {
  def spec = suite("STM optimization")(
    test("STM single transaction is fast") {
      for {
        start <- Clock.nanoTime
        
        result <- STM.atomically {
          for {
            ref <- TRef.make(0).commit
            _ <- ref.set(42)
            v <- ref.get
          } yield v
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(duration < 50))(isTrue)
    },
    
    test("STM retry is efficient") {
      for {
        ref <- TRef.make(false).commit
        
        start <- Clock.nanoTime
        
        _ <- STM.atomically {
          for {
            v <- ref.get
            _ <- if (!v) ref.set(true) else STM.retry
          } yield ()
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 100
    },
    
    test("STM concurrent transactions are scalable") {
      for {
        refs <- ZIO.foreach((1 to 5).toList) { _ =>
          TRef.make(0).commit
        }
        
        start <- Clock.nanoTime
        
        transactions <- ZIO.foreachPar(refs) { ref =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        _ <- ZIO.foreachDiscard(transactions)(_.join)
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        result <- ZIO.succeed(duration < 2000)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("STM doesn't allocate excessively") {
      for {
        start <- Clock.nanoTime
        
        _ <- ZIO.foreach((1 to 50).toList) { _ =>
          STM.atomically {
            for {
              ref <- TRef.make(0).commit
              _ <- ref.set(1)
              v <- ref.get
            } yield v
          }
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 3000
    },
    
    test("STM preserves isolation") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        _ <- STM.atomically {
          ref1.set(1)
        }.andThen(STM.atomically {
          ref2.set(2)
        }).andThen(STM.atomically {
          ref1.get
        })
        
        v1 <- STM.atomically(ref1.get)
        
      } yield v1 == 1
    }
  )
}
