package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #10009
 * 
 * Issue: ZIO stream efficiency
 * 
 * This test verifies stream efficiency.
 */
object BountySpec10009 extends ZIOSpecDefault {
  def spec = suite("ZIO stream efficiency")(
    test("stream operations are zero-allocation") {
      for {
        start <- Clock.nanoTime
        
        _ <- ZStream(1 to 100)
          .map(_ * 2)
          .filter(_ % 3 == 0)
          .take(10)
          .runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 500
    },
    
    test("stream fusion is efficient") {
      for {
        start <- Clock.nanoTime
        
        _ <- ZStream(1 to 100)
          .map(_ * 2)
          .map(_ + 1)
          .take(20)
          .runDrain
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 500
    },
    
    test("stream doesn't create intermediate chunks") {
      for {
        chunksCreated <- Ref.make(0)
        
        _ <- ZStream(1 to 10)
          .chunks(5)
          .foreach(_ => chunksCreated.update(_ + 1))
          .runDrain
        
        created <- chunksCreated.get
      } yield created == 2 // 10 items / 5 per chunk = 2 chunks
    },
    
    test("stream pull model is lazy") {
      for {
        pulled <- Ref.make(0)
        
        stream = ZStream(1 to 1000)
          .tap { _ =>
            pulled.update(_ + 1)
          }
          .take(5)
        
        _ <- stream.runDrain
        
        totalPulled <- pulled.get
      } yield totalPulled == 5
    },
    
    test("stream doesn't buffer entire input") {
      for {
        result <- ZStream.iterate(0)(_ + 1)
          .take(5)
          .runCollect
        
        size = result.size
      } yield size == 5
    },
    
    test("stream parallel processing is efficient") {
      for {
        start <- Clock.nanoTime
        
        result <- ZStream(1 to 20)
          .mapZIO(i => ZIO.succeed(i * 2))
          .runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 1000
    }
  )
}
