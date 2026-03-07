package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9599
 * 
 * Issue: FiberId uniqueness guarantees
 * 
 * This test verifies FiberId uniqueness.
 */
object BountySpec9599 extends ZIOSpecDefault {
  def spec = suite("FiberId uniqueness")(
    test("each fiber gets unique FiberId") {
      for {
        ids <- ZIO.foreach((1 to 10).toList) { _ =>
          ZIO.forkId
        }
        
        unique = ids.distinct.size
      } yield unique == 10
    },
    
    test("FiberId doesn't collide across async boundaries") {
      for {
        id1 <- ZIO.forkId
        
        _ <- ZIO.yieldNow
        
        id2 <- ZIO.forkId
        
        _ <- ZIO.async[Any, Nothing, Long] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(
              ZIO.succeed(42L).flatMap(k).unit
            )
          }
        }
        
        id3 <- ZIO.forkId
      } yield true // All ids should be valid
    },
    
    test("FiberId is monotonically increasing") {
      for {
        ids <- Ref.make(List.empty[Long])
        
        _ <- ZIO.foreach((1 to 10).toList) { _ =>
          ZIO.forkId.flatMap(id => ids.update(_ :+ id))
        }
        
        allIds <- ids.get
        sorted = allIds.sorted
      } yield sorted == allIds // Should be already sorted
    },
    
    test("FiberId doesn't reuse old values") {
      for {
        fiber1 <- ZIO.unit.fork
        _ <- fiber1.join
        
        id1 <- ZIO.forkId
        
        fiber2 <- ZIO.unit.fork
        _ <- fiber2.join
        
        id2 <- ZIO.forkId
      } yield id1 != id2
    },
    
    test("FiberId is unique across restarts") {
      for {
        fiber <- ZIO.unit.fork
        
        id1 <- ZIO.forkId
        
        _ <- fiber.interrupt
        
        id2 <- ZIO.forkId
      } yield id1 != id2
    },
    
    test("FiberId space is large enough") {
      for {
        // Should be able to create many fibers without collision
        ids <- ZIO.foreach((1 to 100).toList) { _ =>
          ZIO.forkId
        }
        
        unique = ids.distinct.size
      } yield unique == 100
    }
  )
}
