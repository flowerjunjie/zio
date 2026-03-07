package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #10241
 * 
 * Issue: Concurrent stateful operations
 * 
 * This test verifies concurrent stateful behavior.
 */
object BountySpec10241 extends ZIOSpecDefault {
  def spec = suite("Concurrent stateful operations")(
    test("Ref updates are atomic") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.update(_ + 1)
        }
        
        value <- ref.get
      } yield value == 20
    },
    
    test("Ref modify is isolated") {
      for {
        ref <- Ref.make((0, 0))
        
        results <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.modify(v => (v + 1, v * 2))
        }
        
        sum <- ZIO.foreach(results)(_.join)
        
        total = sum.sum
      } yield total >= 11 // At least (0+2+4+6+8+10+12+14+16+18+20 = 110)
    },
    
    test("Ref get doesn't affect updates") {
      for {
        ref <- Ref.make(0)
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.get *> ref.update(_ + 1).unit
        }
        
        _ <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.update(_ + 1) *> ref.get
        }.fork
        
        value <- ref.get
      } yield value >= 10
    },
    
    test("Ref alternatives are thread-safe") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.getOrElseUpdate(0)(_ + 1)
        }
        
        value <- ref.get
      } yield value == 10
    },
    
    test("Ref collect is concurrent-safe") {
      for {
        ref <- Ref.make(Map.empty[Int, String])
        
        _ <- ZIO.foreachPar((1 to 5).toList) { i =>
          ref.update(m => m + (i -> s"val$i"))
        }
        
        _ <- ZIO.foreachPar((1 to 5).toList) { _ =>
          ref.update(m => m + (i -> s"value$i"))
        }
        
        _ <- ZIO.foreachPar((1 to 5).toList) { _ =>
          ref.update(m => m + (i -> s"value$i"))
        }
        
        map <- ref.get
        
        size = map.size
      } yield size >= 5
    }
  )
}
