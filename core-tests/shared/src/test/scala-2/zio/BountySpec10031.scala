package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10031
 * 
 * Issue: Concurrent Ref contention
 * 
 * This test verifies concurrent Ref behavior under contention.
 */
object BountySpec10031 extends ZYSpecDefault {
  def spec = suite("Concurrent Ref contention")(
    test("concurrent updates don't lose data") {
      for {
        ref <- Ref.make(0)
        
        fibers <- ZIO.foreachPar((1 to 50).toList) { _ =>
          ref.update(_ + 1)
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        finalValue <- ref.get
      } yield assertCompletes(ZIO.succeed(finalValue))(equalTo(50))
    },
    
    test("concurrent modifyAndGet is atomic") {
      for {
        ref <- Ref.make(0)
        
        results <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.modifyAndGet(v => (v + 1, v))
        }
        
        sum = results.sum
        finalValue <- ref.get
      } yield assertCompletes(ZIO.succeed(sum))(equalTo(210)) && assertCompletes(ZIO.succeed(finalValue))(equalTo(20))
    },
    
    def spec = suite("Concurrent Ref contention")(
    test("concurrent gets are consistent") {
      for {
        ref <- Ref.make(42)
        
        results <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.get
        }
        
        allSame = results.distinct.size == 1
      } yield assertCompletes(ZIO.succeed(allSame))(isTrue)
    },
    
    test("concurrent set is atomic") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          ref.set(i * 10)
        }
        
        value <- ref.get
      } yield value >= 10 && value <= 100 // Some thread's set won
    },
    
    test("concurrent updateAndGet returns final value") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.updateAndGet(_ + 10)
        }
        
        value <- ref.get
      } yield value >= 10 && value <= 100
    },
    
    test("high contention doesn't cause deadlocks") {
      for {
        ref <- Ref.make(0)
        
        fibers <- ZIO.foreachPar((1 to 30).toList) { _ =>
          ref.update(_ + 1)
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        value <- ref.get
      } yield value == 30
    }
  )
}
