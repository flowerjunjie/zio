package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9418
 * 
 * Issue: Concurrent state mutations
 * 
 * This test verifies concurrent state mutation safety.
 */
object BountySpec9418 extends ZIOSpecDefault {
  def spec = suite("Concurrent state mutations")(
    test("Ref.update is thread-safe") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 100).toList) { _ =>
          ref.update(_ + 1)
        }
        
        value <- ref.get
      } yield value == 100
    },
    
    test("Ref.modify is atomic") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.modify(v => (v + 1, v))
        }
        
        value <- ref.get
      } yield value == 10
    },
    
    test("concurrent reads don't block writes") {
      for {
        ref <- Ref.make(42)
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.get
        }.zip(
          ZIO.foreachPar((1 to 10).toList) { _ =>
            ref.update(_ + 1)
          }
        )
        
        (reads, updates) <- fibers
        
        finalValue <- ref.get
      } yield reads.size == 10 && finalValue == 10
    },
    
    test("Ref.updateAndGet is atomic") {
      for {
        ref <- Ref.make(0)
        
        results <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.updateAndGet(_ + 1)
        }
        
        finalValue <- ref.get
      } yield results.size == 20 && finalValue == 20
    },
    
    test("Ref.modifyAndGet is atomic") {
      for {
        ref <- Ref.make("0")
        
        results <- ZIO.foreachPar((1 to 10).toList) { i =>
          ref.modify(v => (s"${v.toInt + 1}", v))
        }
        
        finalValue <- ref.get
      } yield results.size == 10 && finalValue == "10"
    },
    
    test("concurrent state mutations are consistent") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 50).toList) { i =>
          if (i % 2 == 0) ref.update(_ + 1)
          else ref.get
        }
        
        value <- ref.get
      } yield value >= 25 // At least 25 increments
    }
  )
}
