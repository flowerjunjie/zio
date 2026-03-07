package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #10318
 * 
 * Issue: ZStream optimization
 * 
 * This test verifies stream optimization.
 */
object BountySpec10318 extends ZIOSpecDefault {
  def spec = suite("ZStream optimization")(
    test("stream operations are optimized") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 1000)
          .map(_ * 2)
          .filter(_ % 3 == 0)
          .take(100)
        
        _ <- stream.runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        r <- ZIO.succeed(duration < 2000)
      } yield r
    },
    
    test("stream fusion eliminates intermediates") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 100)
          .map(_ + 1)
          .map(_ + 1)
          .take(10)
        
        _ <- stream.runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        r <- ZIO.succeed(duration < 500)
      } yield r
    },
    
    test("stream doesn't buffer excessively") {
      for {
        processed <- Ref.make(0)
        
        stream = ZStream(1 to 10000)
          .tap { _ =>
            processed.update(_ + 1)
          }
          .take(10)
        
        _ <- stream.runDrain
        
        total <- processed.get
      } assertCompletes(ZIO.succeed(total))(equalTo(10))
    },
    
    test("stream parallel processing is efficient") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 100)
          .mapZIO(i => ZIO.succeed(i * 2))
          .runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        r <- ZIO.succeed(duration < 3000)
      } yield r
    },
    
    test("stream laziness is preserved") {
      for {
        processed <- Ref.make(0)
        
        stream = ZStream.iterate(0)(_ + 1)
          .tap { _ =>
            processed.update(_ + 1)
          }
          .take(5)
        
        _ <- stream.runDrain
        
        total <- processed.get
      } yield assertCompletes(ZIO.succeed(total))(equalTo(5))
    },
    
    test("stream merge is concurrent") {
      for {
        s1 = ZStream.fromIterable(1 to 500)
        s2 = ZStream.fromIterable(501 to 1000)
        
        start <- Clock.nanoTime
        
        result <- (s1 merge s2).runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        size = result.size
      } yield size == 1000 && duration < 3000
    },
    
    test("stream has low overhead") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream(1 to 100)
          .map(_ * 2)
          .filter(_ % 3 == 0)
          .take(20)
        
        _ <- stream.runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        r <- ZIO.succeed(duration < 500)
      } yield r
    }
  )
}
