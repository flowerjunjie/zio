package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9240
 * 
 * Issue: FiberId consistency across async boundaries
 * 
 * This test verifies FiberId remains consistent.
 */
object BountySpec9240 extends ZIOSpecDefault {
  def spec = suite("FiberId consistency")(
    test("FiberId is consistent across async boundaries") {
      for {
        fiberId1 <- ZIO.forkId
        
        _ <- ZIO.sleep(10.millis)
        
        fiberId2 <- ZIO.forkId
        
        _ <- ZIO.yieldNow
        
        fiberId3 <- ZIO.forkId
      } yield fiberId1 == fiberId2 && fiberId2 == fiberId3
    },
    
    test("FiberId doesn't change on executeAsync") {
      for {
        id1 <- ZIO.forkId
        
        _ <- ZIO.async[Any, Nothing, Unit] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(k(()))
          }
        }
        
        id2 <- ZIO.forkId
      } yield id1 == id2
    },
    
    test("child fiber has different FiberId") {
      for {
        parent <- ZIO.forkId
        
        childId <- ZIO.forkId.fork.flatMap(_.join)
      } yield parent != childId
    },
    
    test("FiberId is globally unique") {
      for {
        ids <- ZIO.foreach((1 to 10).toList) { _ =>
          ZIO.forkId
        }
        
        unique = ids.distinct.size
      } yield unique == 10
    },
    
    test("FiberId persists across fiber lifetime") {
      for {
        fiber <- ZIO.forkId.repeat(Schedule.recurs(5)).fork
        
        ids <- Ref.make(List.empty[Long])
        
        _ <- (ZIO.forkId.flatMap { id =>
          ids.update(_ :+ id)
        }).repeat(Schedule.recurs(5)).fork.flatMap(_.join)
        
        allIds <- ids.get
        unique = allIds.distinct.size
      } yield unique == 1
    }
  )
}
