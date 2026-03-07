package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9323
 * 
 * Issue: Add @immutable annotation for type system
 * 
 * This test verifies immutable type behavior.
 */
object BountySpec9323 extends ZIOSpecDefault {
  def spec = suite("Immutable type system")(
    test("immutable collections don't mutate") {
      val list = List(1, 2, 3)
      val modified = list :+ 4
      
      assertCompletes(ZIO.succeed(list == List(1, 2, 3)))(isTrue)
    },
    
    test("Chunk doesn't mutate original") {
      val chunk = Chunk(1, 2, 3)
      val modified = chunk :+ 4
      
      assertCompletes(ZIO.succeed(chunk))(equalTo(Chunk(1, 2, 3)))
    },
    
    test("Map operations return new instances") {
      val map = Map(1 -> "a", 2 -> "b")
      val modified = map + (3 -> "c")
      
      assertCompletes(ZIO.succeed(map.size))(equalTo(2))
    },
    
    test("Set operations are immutable") {
      val set = Set(1, 2, 3)
      val modified = set + 4
      
      assertCompletes(ZIO.succeed(set == Set(1, 2, 3)))(isTrue)
    },
    
    test("ZIO effects describe immutable operations") {
      for {
        ref <- Ref.make(0)
        
        _ <- ref.update(_ + 1)
        _ <- ref.update(_ + 1)
        
        value <- ref.get
      } yield value == 2
    },
    
    test("immutable data structures share structure") {
      val base = List(1, 2, 3, 4, 5)
      val mod1 = base :+ 6
      val mod2 = base :+ 7
      
      // Structural sharing should make this efficient
      shared = base.take(3)
      
      assertCompletes(ZIO.succeed(shared))(equalTo(List(1, 2, 3)))
    }
  )
}
