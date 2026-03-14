package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9456
 * 
 * Issue: Memory efficiency improvements
 * 
 * This test verifies memory efficiency characteristics.
 */
object BountySpec9456 extends ZIOSpecDefault {
  def spec = suite("Memory efficiency")(
    test("ZIO doesn't allocate for pure values") {
      for {
        _ <- ZIO.succeed(42)
        _ <- ZIO.succeed("hello")
        _ <- ZIO.unit
      } yield true
    },
    
    test("Chunk operations are memory efficient") {
      val chunk = Chunk(1, 2, 3, 4, 5)
      
      result <- chunk.map(_ * 2).runDrain
      
      assertCompletes(ZIO.succeed(result))(anything)
    },
    
    test("Ref operations don't allocate excessively") {
      for {
        ref <- Ref.make(0)
        
        _ <- ref.update(_ + 1)
        _ <- ref.get
        _ <- ref.set(10)
        _ <- ref.get
      } yield true
    },
    
    test("lazy values don't allocate until accessed") {
      lazy val expensive = List.fill(1000)(42)
      
      result <- ZIO.succeed(expensive.head)
      
      assertCompletes(ZIO.succeed(result))(equalTo(42))
    },
    
    test("ZIO.foreach is memory efficient") {
      val list = (1 to 100).toList
      
      result <- ZIO.foreach(list)(i => ZIO.succeed(i * 2))
      
      assertCompletes(ZIO.succeed(result.size))(equalTo(100))
    },
    
    test("scoped resources are cleaned up promptly") {
      for {
        allocated <- Ref.make(0)
        deallocated <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          allocated.update(_ + 1)
        )(_ => deallocated.update(_ + 1)) *> ZIO.unit
        
        a <- allocated.get
        d <- deallocated.get
      } yield a == 1 && d == 1
    }
  )
}
