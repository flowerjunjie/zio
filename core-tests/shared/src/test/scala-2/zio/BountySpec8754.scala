package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Chunk

/**
 * Test suite for issue #8754
 * 
 * Issue: zio-test: Comparing Chunk to Seq causes ClassCastException
 * 
 * This test verifies that comparing Chunk to Seq in test assertions
 * doesn't cause ClassCastException.
 */
object BountySpec8754 extends ZIOSpecDefault {
  def spec = suite("Chunk to Seq comparison")(
    test("comparing Chunk[Int] to Seq[Int] works") {
      val chunk = Chunk(1, 2, 3)
      val seq: Seq[Int] = List(1, 2, 3)
      
      // This should not cause ClassCastException
      assertCompletes(ZIO.succeed(chunk))(equalTo(seq))
    },
    
    test("comparing Chunk[String] to Seq[String] works") {
      val chunk = Chunk("a", "b", "c")
      val seq: Seq[String] = List("a", "b", "c")
      
      assertCompletes(ZIO.succeed(chunk))(equalTo(seq))
    },
    
    test("comparing empty Chunk to Seq works") {
      val chunk = Chunk.empty[Int]
      val seq: Seq[Int] = Nil
      
      assertCompletes(ZIO.succeed(chunk))(equalTo(seq))
    },
    
    test("nested Chunk comparisons work") {
      val chunkOfChunks = Chunk(Chunk(1, 2), Chunk(3, 4))
      val seqOfSeqs = Seq(Seq(1, 2), Seq(3, 4))
      
      assertCompletes(ZIO.succeed(chunkOfChunks))(equalTo(seqOfSeqs))
    },
    
    test("Chunk can be compared to List specifically") {
      val chunk = Chunk(1, 2, 3)
      val list = List(1, 2, 3)
      
      assertCompletes(ZIO.succeed(chunk))(equalTo(list))
    },
    
    test("Chunk can be compared to Vector") {
      val chunk = Chunk(1, 2, 3)
      val vector = Vector(1, 2, 3)
      
      assertCompletes(ZIO.succeed(chunk))(equalTo(vector))
    }
  )
}
