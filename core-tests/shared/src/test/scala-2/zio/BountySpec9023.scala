package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Chunk

/**
 * Test suite for issue #9023
 * 
 * Issue: Concatenating a Chunk[Int] with Chunk[Long] results in a ClassCastException
 * 
 * This test verifies Chunk concatenation type safety.
 */
object BountySpec9023 extends ZIOSpecDefault {
  def spec = suite("Chunk concatenation type safety")(
    test("concatenating same type Chunks works") {
      val chunk1 = Chunk(1, 2, 3)
      val chunk2 = Chunk(4, 5, 6)
      val result = chunk1 ++ chunk2
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3, 4, 5, 6)))
    },
    
    test("concatenating Chunk[Int] with Chunk[Long] should be safe") {
      val chunkInt = Chunk(1, 2, 3)
      val chunkLong = Chunk(4L, 5L, 6L)
      
      // This should work without ClassCastException
      val result = chunkInt ++ chunkLong
      
      assertCompletes(ZIO.succeed(result))(hasSize(equalTo(6)))
    },
    
    test("concatenating Chunks preserves element types") {
      val chunk1 = Chunk("a", "b")
      val chunk2 = Chunk("c", "d")
      val result = chunk1 ++ chunk2
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk("a", "b", "c", "d")))
    },
    
    test("concatenating empty Chunks works") {
      val chunk1 = Chunk.empty[Int]
      val chunk2 = Chunk(1, 2, 3)
      val result = chunk1 ++ chunk2
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3)))
    },
    
    test("concatenating multiple Chunks") {
      val chunks = List(
        Chunk(1, 2),
        Chunk(3, 4),
        Chunk(5, 6)
      )
      
      val result = chunks.reduce(_ ++ _)
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3, 4, 5, 6)))
    }
  )
}
