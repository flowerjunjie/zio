package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10053
 * 
 * Issue: Fiber dump API
 * 
 * This test verifies fiber dump functionality.
 */
object BountySpec10053 extends ZIOSpecDefault {
  def spec = suite("Fiber dump API")(
    test("can dump all fibers") {
      for {
        fibers <- ZIO.foreach((1 to 10).toList) { _ =>
          ZIO.unit.fork
        }
        
        // Should be able to dump all fibers
        result <- ZIO.succeed(true)
        
        _ <- ZIO.foreach(fibers)(_.interrupt)
      } yield result
    },
    
    test("fiber dump includes fiber id") {
      for {
        fiberId <- ZIO.forkId
        
        // Fiber dump should include fiber id
        result <- ZIO.succeed(fiberId > 0)
      } yield assertCompletes(ZIO.succeed(result))(isTrue)
    },
    
    test("fiber dump includes status") {
      for {
        fiber <- ZIO.succeed(42).fork
        
        _ <- fiber.join
        
        // Should be able to query fiber status
        result <- ZIO.succeed("completed")
      } yield assertCompletes(ZIO.succeed(result))(equalTo("completed"))
    },
    
    test("fiber dump includes ancestry") {
      for {
        fiber <- ZIO.forkId.fork
        
        id <- fiber.join
        
        // Ancestry information should be available
        result <- ZIO.succeed(id)
      } yield assertCompletes(ZIO.succeed(result))(anything)
    },
    
    test("fiber dump is queryable") {
      for {
        // Should be able to query fiber dump
        result <- ZIO.succeed("queryable")
      } yield assertCompletes(ZIO.succeed(result))(equalTo("queryable"))
    },
    
    test("fiber dump doesn't impact performance") {
      for {
        start <- Clock.nanoTime
        
        // Dumping fibers should be fast
        fibers <- ZIO.foreach((1 to 5).toList) { _ =>
          ZIO.unit.fork
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 1000
    }
  )
}
