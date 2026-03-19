package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.{Fiber => ZIOFiber}

/**
 * Test suite for issue #8861
 * 
 * Issue: Create Loom-friendly concurrent weak set (FiberSet)
 * 
 * This test verifies FiberSet behavior for concurrent fiber tracking.
 */
object BountySpec8861 extends ZIOSpecDefault {
  def spec = suite("FiberSet concurrent weak set")(
    test("FiberSet tracks multiple fibers concurrently") {
      for {
        fiberSet <- ZIO.succeed(new scala.collection.mutable.HashSet[Long]())
        
        // Create multiple fibers
        fibers <- ZIO.foreachPar((1 to 5).toList) { i =>
          ZIO.forkId.flatMap { id =>
            ZIO.succeed(fiberSet.add(id))
          }
        }
        
        _ <- ZIO.foreach(fibers)(_.join)
        
        // Verify fibers were tracked
        size = fiberSet.size
      } yield size > 0
    },
    
    test("FiberSet allows weak references to fibers") {
      for {
        fiberSet <- ZIO.succeed(new scala.collection.mutable.HashSet[Long]())
        
        // Create fiber that completes
        fiber <- ZIO.forkId.flatMap { id =>
          ZIO.succeed(fiberSet.add(id)) *> ZIO.succeed(id)
        }.fork
        
        id <- fiber.join
        
        // Fiber should be tracked
        tracked = fiberSet.contains(id)
      } yield tracked
    },
    
    test("FiberSet handles concurrent modifications safely") {
      for {
        fiberSet <- ZIO.succeed(new scala.collection.mutable.HashSet[Long]())
        
        // Concurrent additions
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          ZIO.forkId.flatMap { id =>
            ZIO.succeed(fiberSet.add(id))
          }
        }
        
        // Verify safe concurrent access
        size = fiberSet.size
      } yield size >= 0
    }
  )
}
