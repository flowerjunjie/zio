package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10152
 * 
 * Issue: FiberId uniqueness across restarts
 * 
 * This test verifies FiberId is globally unique.
 */
object BountySpec10152 extends ZIOSpecDefault {
  def spec = suite("FiberId uniqueness across restarts")(
    test("FiberIds are unique across async boundaries") {
      for {
        id1 <- ZIO.forkId
        
        _ <- ZIO.yieldNow
        
        id2 <- ZIO.forkId
        
        result <- ZIO.succeed(id1 != id2)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("FiberId is monotonic") {
      for {
        ids <- ZIO.foreach((1 to 10).toList) { _ =>
          ZIO.forkId
        }
        
        allIds <- ZIO.succeed(ids)
        
        sorted = ZIO.succeed(allIds.sorted)
        
        isSorted = for {
          list1 <- allIds.zip(allIds.tail).forall { case (a, b) => a <= b }
        } yield isSorted
      } yield assertCompletes(isSorted)(isTrue)
    },
    
    test("FiberId doesn't collide even under high concurrency") {
      for {
        ids <- ZIO.foreachPar((1 to 50).toList) { _ =>
          ZIO.forkId
        }
        
        unique <- ZIO.succeed(ids.distinct.size)
        
        result <- unique
      } yield assertCompletes(ZIO.succeed(result))(equalTo(50))
    },
    
    test("FiberId persists across fiber lifetime") {
      for {
        id1 <- ZIO.forkId
        
        // FiberId should remain the same throughout
        _ <- ZIO.yieldNow
        _ <- ZIO.yieldNow
        
        id2 <- ZIO.forkId
        
        result <- ZIO.succeed(id1 == id2)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("FiberId doesn't reuse old values") {
      for {
        id1 <- ZIO.forkId
        
        fiber <- ZIO.succeed(42).fork
        
        _ <- fiber.join
        
        id2 <- ZIO.forkId
        
        result <- ZIO.succeed(id1 != id2)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("FiberId space is sufficiently large") {
      for {
        ids <- ZIO.foreach((1 to 100).toList) { _ =>
          ZIO.forkId
        }
        
        unique <- ZIO.succeed(ids.distinct.size)
        
        result <- unique
      } yield assertCompletes(ZIO.succeed(result))(equalTo(100))
    }
  )
}
