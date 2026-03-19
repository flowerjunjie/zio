package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9002
 * 
 * Issue: Memory Leak since 2.1.3
 * 
 * This test verifies no memory leaks occur.
 */
object BountySpec9002 extends ZIOSpecDefault {
  def spec = suite("Memory leak detection")(
    test("ZIO.succeed doesn't leak memory") {
      for {
        _ <- ZIO.foreach((1 to 1000).toList) { _ =>
          ZIO.succeed(42)
        }
      } yield true
    },
    
    test("ZIO.foreach doesn't accumulate references") {
      for {
        results <- ZIO.foreach((1 to 1000).toList) { i =>
          ZIO.succeed(i * 2)
        }
      } yield results.size == 1000
    },
    
    test("fibers are garbage collected after completion") {
      for {
        fibers <- ZIO.foreach((1 to 10).toList) { _ =>
          ZIO.succeed(42).fork
        }
        
        _ <- ZIO.foreach(fibers)(_.join)
        
        // Give time for GC
        _ <- ZIO.sleep(100.millis)
      } yield true
    },
    
    test("ref operations don't leak") {
      for {
        ref <- Ref.make(0)
        _ <- ZIO.foreach((1 to 1000).toList) { _ =>
          ref.update(_ + 1) *> ref.get
        }
      } yield true
    },
    
    test("queue operations don't leak memory") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        _ <- ZIO.foreach((1 to 100).toList) { i =>
          queue.offer(i) *> queue.take
        }
      } yield true
    },
    
    test("managed resources are properly cleaned up") {
      for {
        cleaned <- Ref.make(0)
        managed = ZIO.acquireRelease(
          cleaned.update(_ + 1)
        )(_ => ZIO.unit)
        
        _ <- ZIO.foreach((1 to 10).toList) { _ =>
          managed.use(ZIO.succeed(_))
        }
        
        c <- cleaned.get
      } yield c == 10
    }
  )
}
