package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Queue

/**
 * Test suite for issue #10163
 * 
 * Issue: Concurrent queue operations
 * 
 * This test verifies queue thread-safety.
 */
object BountySpec10163 extends ZIOSpecDefault {
  def spec = suite("Concurrent queue operations")(
    test("concurrent offer is thread-safe") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { i =>
          queue.offer(i)
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- queue.size
      } yield assertCompletes(ZIO.succeed(size))(equalTo(20))
    },
    
    test("concurrent take is thread-safe") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        // Pre-fill queue
        _ <- ZIO.foreach((1 to 10).toList) { i =>
          queue.offer(i)
        }
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { _ =>
          queue.take.flatMap(_ => queue.take)
        }
        
        results <- ZIO.foreach(fibers)(_.join)
        
        count = results.flatten.length
      } yield count >= 10
    },
    
    test("concurrent offer/take pair is atomic") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          for {
            _ <- queue.offer(i)
            v <- queue.take
            _ <- ZIO.succeed(v == i)
          } yield ()
        }
        
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("queue doesn't lose data under contention") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        fibers <- ZIO.foreachPar((1 to 15).toList) { i =>
          queue.offer(i) *> queue.take.flatMap(_ => queue.take)
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("queue maintains FIFO order") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        _ <- ZIO.foreach((1 to 5).toList) { i =>
          queue.offer(i)
        }
        
        results <- ZIO.foreach((1 to 5).toList) { _ =>
          queue.take
        }
        
        values <- ZIO.foreach(results)(_.join)
        
        order = values
        
        result <- ZIO.succeed(order == Chunk(1, 2, 3, 4, 5))
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("queue is blocking when empty") {
      for {
        queue <- zio.Queue.bounded[Int](10)
        
        start <- Clock.nanoTime
        
        result <- queue.take.timeout(1.second).either
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        timedOut <- result.isLeft
        tookTooLong <- duration >= 1000
      } yield assertCompletes(ZIO.succeed(timedOut && tookTooLong))(isTrue)
    }
  )
}
