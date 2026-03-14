package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #8983
 * 
 * Issue: Using STM is much slower than using a single Ref even with contention
 * 
 * This test verifies STM performance characteristics.
 */
object BountySpec8983 extends ZIOSpecDefault {
  def spec = suite("STM performance")(
    test("STM commit is fast for single transaction") {
      for {
        start <- Clock.nanoTime
        _ <- ZIO.foreach((1 to 100).toList) { _ =>
          STM.atomically {
            for {
              ref <- TRef.make(0)
              _ <- ref.set(1)
            } yield ()
          }
        }
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 1000.0 // Should complete in under 1 second
    },
    
    test("STM is faster than Ref for concurrent writes") {
      for {
        ref <- Ref.make(0)
        
        // Ref version
        start1 <- Clock.nanoTime
        fibers1 <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.update(_ + 1)
        }.fork
        _ <- fibers1.join
        end1 <- Clock.nanoTime
        
        refValue1 <- ref.get
        
        // STM version
        tref <- TRef.make(0).commit
        start2 <- Clock.nanoTime
        fibers2 <- ZIO.foreachPar((1 to 10).toList) { _ =>
          STM.atomically(tref.update(_ + 1))
        }.fork
        _ <- fibers2.join
        end2 <- Clock.nanoTime
        
        refValue2 <- STM.atomically(tref.get)
        
        duration1 = (end1 - start1) / 1000000.0
        duration2 = (end2 - start2) / 1000000.0
      } yield refValue1 == 10 && refValue2 == 10 && duration2 < duration1 * 2
    },
    
    test("STM retry doesn't cause excessive overhead") {
      for {
        start <- Clock.nanoTime
        _ <- ZIO.foreach((1 to 50).toList) { _ =>
          STM.atomically {
            for {
              ref1 <- TRef.make(1)
              ref2 <- TRef.make(2)
              v1 <- ref1.get
              v2 <- ref2.get
            } yield v1 + v2
            }
          }
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 500.0
    },
    
    test("STM scales with concurrent transactions") {
      for {
        tref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { _ =>
          STM.atomically {
            for {
              _ <- tref.update(_ + 1)
              value <- tref.get
            } yield value
          }
        }
        
        sum <- fibers
        finalValue <- STM.atomically(tref.get)
      } yield sum.size == 20 && finalValue == 20
    }
  )
}
