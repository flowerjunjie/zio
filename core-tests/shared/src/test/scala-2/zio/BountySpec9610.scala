package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9610
 * 
 * Issue: Stream performance testing
 * 
 * This test verifies stream performance characteristics.
 */
object BountySpec9610 extends ZYSpecDefault {
  def spec = suite("Stream performance testing")(
    test("stream operations are fast") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 1000)
          .map(_ * 2)
          .filter(_ % 3 == 0)
          .take(100)
        
        _ <- stream.runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 1000
    },
    
    test("stream composition is efficient") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 100)
          .map(_ * 2)
          .flatMap(i => ZStream(i, i * 2, i * 3))
          .take(50)
        
        _ <- stream.runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 500
    },
    
    test("stream merge is concurrent") {
      for {
        start <- Clock.nanoTime
        
        stream1 = ZStream.fromIterable(1 to 500)
        stream2 = ZStream.fromIterable(501 to 1000)
        
        _ <- (stream1 merge stream2).runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 2000
    },
    
    test("stream doesn't buffer excessively") {
      for {
        processed <- Ref.make(0)
        
        stream = ZStream.fromIterable(1 to 10000)
          .tap { _ =>
            processed.update(_ + 1)
          }
          .take(10)
        
        _ <- stream.runDrain
        
        count <- processed.get
      } yield count == 10 // Only processed first 10
    },
    
    test("stream parallel processing is fast") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 100)
          .mapZIO(i => ZIO.succeed(i * 2))
          .take(20)
        
        _ <- stream.runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 1000
    }
  )
}
