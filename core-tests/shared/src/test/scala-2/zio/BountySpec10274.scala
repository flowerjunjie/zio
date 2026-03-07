package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Test suite for issue #10274
 * 
 * Issue: Concurrent deque operations
 * 
 * This test verifies concurrent deque behavior.
 */
object BountySpec10274 extends ZIOSpecDefault {
  def spec = suite("Concurrent deque operations")(
    test("concurrent add is thread-safe") {
      for {
        deque = new ConcurrentLinkedDeque[Int]()
        
        _ <- ZIO.foreachPar((1 to 20).toList) { i =>
          ZIO.succeed(deque.addLast(i))
        }
        
        size = deque.size
        
        _ <- ZIO.foreachPar((1 to 5).toList) { _ =>
          deque.pollFirst()
          deque.pollFirst()
        }
        
        finalSize <- ZIO.succeed(deque.size)
      } yield size == 15 && finalSize == 10
    },
    
    test("concurrent addLast is thread-safe") = {
      for {
        deque = new ConcurrentLinkedDeque[Int]()
        
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          ZIO.succeed(deque.addLast(i))
        }
        
        size <- ZIO.succeed(deque.size)
      } yield size == 10
    },
    
    test("concurrent pollFirst doesn't block") {
      for {
        deque = new ConcurrentLinkedDeque[Int]()
        
        _ <- (1 to 10).foreach { i =>
          ZIO.succeed(deque.addLast(i))
        }
        
        start <- Clock.nanoTime
        
        results <- ZIO.foreach((1 to 5).toList) { _ =>
          deque.pollFirst()
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        firstResult <- results(0)
        
        r <- ZIO.succeed(firstResult.isDefined && duration < 1000)
      } yield r
    },
    
    test("concurrent pollLast is isolated") {
      for {
        deque = new ConcurrentLinkedDeque[Int]()
        
        _ <- (1 to 5).foreach { i =>
          ZIO.succeed(deque.addLast(i))
        }
        
        results <- ZIO.foreachPar((1 to 5).toList) { _ =>
          deque.pollLast()
          deque.pollLast()
        }
        
        allDefined <- ZIO.foreach(results)(r => ZIO.succeed(r.isDefined))
        
        allDefs = allDefined.forall(r => r.isDefined)
      } yield allDefs
    },
    
    test("concurrent remove is safe") {
      for {
        deque = new ConcurrentLinkedDeque[Int]()
        
        _ <- (1 to 10).foreach { i =>
          ZIO.succeed(deque.addLast(i))
        }
        
        _ <- ZIO.foreachPar((1 to 5).toList) { _ =>
          deque.remove(3)
          deque.remove(3)
        }
        
        results <- ZIO.foreach((1 to 5).toList) { _ =>
          deque.remove(3)
          deque.remove(3)
        }.fork
        
        _ <- ZIO.foreach(results)(_.join)
        
        finalSize <- ZIO.succeed(new ConcurrentLinkedDeque[Int]().size)
      } yield finalSize >= 5
    }
  )
}
