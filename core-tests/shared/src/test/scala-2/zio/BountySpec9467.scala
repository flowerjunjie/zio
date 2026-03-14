package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9467
 * 
 * Issue: Concurrent Ref operations safety
 * 
 * This test verifies concurrent Ref operation safety.
 */
object BountySpec9467 extends ZIOSpecDefault {
  def spec = suite("Concurrent Ref operations")(
    test("concurrent get operations are consistent") {
      for {
        ref <- Ref.make(42)
        
        results <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.get
        }
        
        allSame = results.distinct.size == 1
      } yield allSame
    },
    
    test("concurrent set operations are atomic") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.set(100)
        }
        
        value <- ref.get
      } yield value == 100 // Should be 100, not some intermediate value
    },
    
    test("concurrent modifyAndGet is thread-safe") {
      for {
        ref <- Ref.make(0)
        
        results <- ZIO.foreachPar((1 to 50).toList) { _ =>
          ref.modifyAndGet(v => (v + 1, v))
        }
        
        finalValue <- ref.get
      } yield finalValue == 50 && results.sum == 1275 // sum of 0+1+2+...+49
    },
    
    test("concurrent updateAndGet is thread-safe") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 30).toList) { _ =>
          ref.updateAndGet(_ + 10)
        }
        
        finalValue <- ref.get
      } yield finalValue == 300 // 30 * 10
    },
    
    test("concurrent alter operations are safe") {
      for {
        ref <- Ref.make(List.empty[Int])
        
        _ <- ZIO.foreachPar((1 to 20).toList) { i =>
          ref.update(_ :+ i)
        }
        
        finalList <- ref.get
        size = finalList.size
      } yield size == 20 && finalList.toSet.size == 20 // All unique
    }
  )
}
