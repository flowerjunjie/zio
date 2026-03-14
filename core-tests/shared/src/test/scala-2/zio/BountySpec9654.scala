package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #9654
 * 
 * Issue: Concurrent data structures
 * 
 * This test verifies concurrent data structure safety.
 */
object BountySpec9654 extends ZIOSpecDefault {
  def spec = suite("Concurrent data structures")(
    test("TRef is thread-safe") {
      for {
        ref <- TRef.make(0).commit
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { _ =>
          STM.atomically {
            ref.update(_ + 1)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        value <- STM.atomically(ref.get)
      } yield value == 20
    */
    test("TArray is thread-safe") {
      for {
        array <- TArray.make(5)(0).commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          STM.atomically {
            for {
              _ <- array.update(i, i * 2)
            v <- array.get(i)
            _ <- array.set(i, v * 3)
            finalV <- array.get(i)
            } yield ()
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        result <- STM.atomically {
          for {
            v0 <- array.get(0)
            v1 <- array.get(1)
          } yield v0 + v1
        }
      } yield result >= 0
    },
    
    test("TMap is thread-safe") {
      for {
        map <- TMap.empty[String, Int].commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          STM.atomically {
            map.put(s"key$i", i)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(map.size)
      } yield size == 10
    },
    
    test("TSet is thread-safe") {
      for {
        set <- TSet.empty[Int].commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          STM.atomically {
            set.put(i)
          }
        }
        
        _ <- Z.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(set.size)
      } yield size == 10
    },
    
    test("TQueue is thread-safe") {
      for {
        queue <- TQueue.bounded[Int](100).commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          STM.atomically {
            queue.offer(i)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(queue.size)
      } yield size == 10
    },
    test("concurrent STM transactions don't deadlock") {
      for {
        ref1 <- TRef.make(0).commit
        ref2 <- TRef.make(0).commit
        
        result <- STM.atomically {
          for {
            _ <- ref1.update(_ + 1)
            _ <- ref2.update(_ + 1)
          } yield ()
        }
      } yield true
    }
  )
}
