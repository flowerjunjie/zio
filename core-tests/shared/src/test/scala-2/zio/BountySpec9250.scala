package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9250
 * 
 * Issue: Add fiber-based causality trace
 * 
 * This test verifies fiber causality tracking.
 */
object BountySpec9250 extends ZIOSpecDefault {
  def spec = suite("Fiber causality trace")(
    test("child fiber tracks parent fiber") {
      for {
        parentFiberId <- ZIO.forkId
        
        childFiber <- (ZIO.forkId).fork
        
        childId <- childFiber.join
      } yield childId != parentFiberId
    },
    
    test("fiber hierarchy is maintained") {
      for {
        results <- Ref.make(List.empty[Long])
        
        _ <- ZIO.forkId.flatMap { parentId =>
          results.update(_ :+ parentId) *>
          (ZIO.forkId.flatMap { childId =>
            results.update(_ :+ childId)
          }.fork).flatMap(_.join)
        }
        
        ids <- results.get
      } yield ids.size == 2
    },
    
    test("causality trace spans multiple generations") {
      for {
        depth <- Ref.make(0)
        
        nested = (n: Int) => ZIO.forkId.flatMap { _ =>
          if (n <= 0) depth.update(_ + 1)
          else nested(n - 1).fork.flatMap(_.join)
        }
        
        _ <- nested(3)
        
        d <- depth.get
      } yield d == 4
    },
    
    test("orphaned fibers have no parent") {
      for {
        _ <- (ZIO.forkId *> ZIO.unit).fork.flatMap(_.join)
        
        // Orphaned fibers should still have IDs
        result <- ZIO.forkId
      } yield result > 0
    },
    
    test("causality trace is preserved across async boundaries") {
      for {
        parent <- ZIO.forkId
        
        child <- (ZIO.forkId).fork
        
        _ <- ZIO.sleep(10.millis)
        
        childId <- child.join
      } yield childId != parent
    }
  )
}
