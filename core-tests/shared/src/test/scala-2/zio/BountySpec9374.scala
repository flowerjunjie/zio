package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9374
 * 
 * Issue: ZStream chunking strategies
 * 
 * This test verifies stream chunking behavior.
 */
object BountySpec9374 extends ZIOSpecDefault {
  def spec = suite("ZStream chunking strategies")(
    test("ZStream.chunks returns consistent chunk sizes") {
      val stream = ZStream(1 to 100)
      
      result <- stream.chunks(10).runCollect
      
      sizes = result.map(_.size)
      
      assertCompletes(ZIO.succeed(sizes))(equalTo(Chunk.fill(10)(10)))
    },
    
    test("ZStream.chunksN handles last chunk") {
      val stream = ZStream(1 to 95)
      
      result <- stream.chunks(10).runCollect
      
      sizes = result.map(_.size)
      
      expected = Chunk.fill(9)(10) :+ 5
    } yield sizes == expected
    },
    
    test("ZStream.chunks with size 1") {
      val stream = ZStream(1, 2, 3, 4, 5)
      
      result <- stream.chunks(1).runCollect
      
      sizes = result.map(_.size)
    } yield sizes == Chunk(1, 1, 1, 1, 1)
    },
    
    test("ZStream.chunks with large size") {
      val stream = ZStream(1 to 100)
      
      result <- stream.chunks(1000).runCollect
      
      assertCompletes(ZIO.succeed(result.size))(equalTo(1))
    },
    
    test("ZStream.chunks preserves order") {
      val stream = ZStream(1 to 50)
      
      result <- stream.chunks(10).runCollect
      
      flattened = result.flatten
      
      assertCompletes(ZIO.succeed(flattened))(equalTo(Chunk.fromIterable(1 to 50)))
    },
    
    test("ZStream.sliding windows work correctly") {
      val stream = ZStream(1 to 10)
      
      result <- stream.sliding(5, 3).runCollect
      
      assertCompletes(ZIO.succeed(result.size))(equalTo(3))
    }
  )
}
