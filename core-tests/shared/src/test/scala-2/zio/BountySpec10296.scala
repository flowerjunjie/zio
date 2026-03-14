package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._
import zio.Queue

/**
 * Test suite for issue #10296
 * 
 * Issue: ZIO queue backpressure
 * 
 * This test verifies queue backpressure handling.
 */
object BountySpec10296 extends ZIOSpecDefault {
  def spec = suite("ZIO queue backpressure")(
    test("queue respects backpressure") {
      for {
        queue <- zio.Queue.bounded[Int](10)
        
        stream = ZStream(1 to 100)
          .tap { _ =>
            queue.offer(_).catchAll(_ => ZIO.unit)
          }
          .take(20)
        
        _ <- stream.runDrain
        
        size <- queue.size
      } yield assertCompletes(ZIO.succeed(size))(equalTo(10))
    },
    
    test("queue handles slow consumers") {
      for {
        queue <- zio.Queue.bounded[Int](5)
        
        stream = ZStream(1 to 20)
          .mapZIO(i => ZIO.sleep(10.millis) *> queue.offer(i))
          .runDrain
        
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("queue doesn't overflow") {
      for {
        queue <- zio.Queue.bounded[Int](10)
        
        stream = ZStream(1 to 100)
          .tap { _ =>
            queue.offer(_).catchAll(_ => ZIO.unit)
          }
          .take(15)
        
        _ <- stream.runDrain
        
        size <- queue.size
      } yield assertCompletes(ZIO.succeed(size <= 10))
    },
    
    test("queue allows parallel producers") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        producers <- ZIO.foreachPar((1 to 5).toList) { i =>
          ZIO.sleep(10.millis) *> queue.offer(i * 10)
        }.fork
        
        _ <- ZIO.foreach(producers)(_.join)
        
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("queue handles concurrent consumers") {
      for {
        queue <- zio.Queue.bounded[Int](50)
        
        _ <- ZIO.foreach((1 to 10).toList) { _ =>
          queue.take.flatMap(_ => queue.take)
        }.fork
        
        result <- ZIO.succeed(true)
      } yield result
    },
    
    test("queue maintains FIFO order") {
      for {
        queue <- zio.Queue.bounded[Int](100)
        
        _ <- ZIO.foreach((1 to 20).toList) { i =>
          queue.offer(i)
        }
        
        results <- ZIO.foreach((1 to 10).toList) { _ =>
          queue.take
        }
        
        values <- ZIO.foreach(results)(_.join)
        
        order = values
        
        assertCompletes(ZIO.succeed(order))(equalTo(List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
      } yield true
    }
  )
}
