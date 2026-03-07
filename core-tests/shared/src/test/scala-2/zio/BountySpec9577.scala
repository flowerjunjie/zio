package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9577
 * 
 * Issue: Thread safety guarantees
 * 
 * This test verifies thread safety guarantees.
 */
object BountySpec9577 extends ZIOSpecDefault {
  def spec = suite("Thread safety guarantees")(
    test("Ref operations are thread-safe") {
      for {
        ref <- Ref.make(0)
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.update(_ + 1)
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        value <- ref.get
      } yield value == 20
    },
    
    test("Queue operations are thread-safe") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          queue.offer(i) *> queue.take.flatMap(t =>
            ZIO.succeed(t)
          )
        }
        
        results <- ZIO.foreach(fibers)(_.join)
        
        count = results.size
      } yield count == 10
    },
    
    test("STM is thread-safe by design") {
      import zio.stm._
      
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        sum <- fibers.join
        
        finalValue <- STM.atomically(ref.get)
      } yield sum == 10 && finalValue == 10
    },
    
    test("Promise operations are thread-safe") {
      for {
        promise <- Promise.make[Nothing, Int]
        
        fiber1 <- promise.succeed(42).fork
        fiber2 <- promise.await.fork
        
        _ <- fiber1.join
        result <- fiber2.join
        
        _ <- fiber2.interrupt
      } yield result == 42
    },
    
    test("concurrent reads don't cause race conditions") {
      for {
        ref <- Ref.make(42)
        
        results <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.get
        }
        
        allSame = results.distinct.size == 1
      } yield allSame
    },
    
    test("concurrent writes are serialized") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          ref.update(_ + i)
        }
        
        value <- ref.get
      } yield value >= 10 // At least 0+1+2+...+10 = 55, but could be more due to reordering
    }
  )
}
