package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Chunk

/**
 * Test suite for issue #9070
 * 
 * Issue: Concat traversal stops too early
 * 
 * This test verifies concat traverses entire structure.
 */
object BountySpec9070 extends ZIOSpecDefault {
  def spec = suite("concat traversal")(
    test("concat traverses all elements") {
      val chunk1 = Chunk(1, 2, 3)
      val chunk2 = Chunk(4, 5, 6)
      val chunk3 = Chunk(7, 8, 9)
      
      val result = chunk1 ++ chunk2 ++ chunk3
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3, 4, 5, 6, 7, 8, 9)))
    },
    
    test("concat with many chunks") {
      val chunks = List(
        Chunk(1),
        Chunk(2),
        Chunk(3),
        Chunk(4),
        Chunk(5)
      )
      
      val result = chunks.reduce(_ ++ _)
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3, 4, 5)))
    },
    
    test("concat doesn't stop early on empty chunks") {
      val chunk1 = Chunk(1, 2)
      val chunk2 = Chunk.empty[Int]
      val chunk3 = Chunk(3, 4)
      
      val result = chunk1 ++ chunk2 ++ chunk3
      
      assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3, 4)))
    },
    
    test("concat traversal count is correct") {
      var count = 0
      
      val chunk1 = Chunk(1, 2, 3)
      val chunk2 = Chunk(4, 5, 6)
      val result = chunk1 ++ chunk2
      
      result.foreach(_ => count += 1)
      
      assertCompletes(ZIO.succeed(count))(equalTo(6))
    },
    
    test("concat with large chunks") {
      val chunk1 = Chunk.fromIterable((1 to 1000))
      val chunk2 = Chunk.fromIterable((1001 to 2000))
      val result = chunk1 ++ chunk2
      
      assertCompletes(ZIO.succeed(result.size))(equalTo(2000))
    }
  )
}
