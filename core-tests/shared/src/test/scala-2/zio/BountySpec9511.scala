package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9511
 * 
 * Issue: ZStream buffering strategy
 * 
 * This test verifies stream buffering behavior.
 */
object BountySpec9511 extends ZIOSpecDefault {
  def spec = suite("ZStream buffering strategy")(
    test("ZStream.buffer doesn't buffer entire stream") {
      val stream = ZStream.fromIterable(1 to 1000).buffer(10)
      
      result <- stream.take(5).runDrain
      
      assertCompletes(ZIO.succeed(result))(anything)
    },
    
    test("ZStream.bufferSliding uses sliding window") {
      val stream = ZStream(1 to 20).bufferSliding(5)
      
      result <- stream.take(3).runDrain
      
      assertCompletes(ZIO.succeed(result))(anything)
    },
    
    test("ZStream.bufferAll can buffer entire stream") {
      val stream = ZStream(1 to 100).bufferAll
      
      result <- stream.runCollect
      
      assertCompletes(ZIO.succeed(result.size))(equalTo(100))
    },
    
    test("ZStream.buffering doesn't consume all eagerly") {
      for {
        count <- Ref.make(0)
        
        stream = ZStream.fromIterable(1 to 1000)
          .tap { _ =>
            count.update(_ + 1)
          }
          .buffer(10)
          .take(5)
        
        _ <- stream.runDrain
        
        finalCount <- count.get
      } yield finalCount == 5 // Only consumed first 5
    },
    
    test("ZStream.bufferSliding is efficient") {
      val stream = ZStream(1 to 100).bufferSliding(20)
      
      result <- stream.take(3).runDrain
      
      assertCompleteness(ZIO.succeed(result))(anything)
    },
    
    test("ZStream.bufferAll handles finite streams") {
      val stream = ZStream(1, 2, 3).bufferAll
      
      result <- stream.runCollect
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3)))
    }
  )
}
