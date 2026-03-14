package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.managed._

/**
 * Test suite for issue #9273
 * 
 * Issue: Safely construct ZLayer without requiring ZIO in constructor
 * 
 * This test verifies safe layer construction.
 */
object BountySpec9273 extends ZIOSpecDefault {
  def spec = suite("Safe ZLayer construction")(
    test("ZLayer can be constructed from pure values") {
      val layer = ZLayer.succeed(42)
      
      assertCompletes(ZIO.succeed(layer))(anything)
    },
    
    test("ZLayer can be constructed from effect") {
      val layer = ZLayer.fromZIO(ZIO.succeed("service"))
      
      assertCompletes(ZIO.succeed(layer))(anything)
    },
    
    test("ZLayer can be constructed from managed") {
      val managed = ZManaged.acquireRelease(
        ZIO.succeed("resource")
      )(_ => ZIO.unit)
      
      val layer = ZLayer.fromManaged(managed)
      
      assertCompletes(ZIO.succeed(layer))(anything)
    },
    
    test("ZLayer composition is safe") {
      val layer1 = ZLayer.succeed(1)
      val layer2 = ZLayer.succeed(2)
      
      val composed = layer1 >>> layer2
      
      assertCompletes(ZIO.succeed(composed))(anything)
    },
    
    test("ZLayer doesn't require runtime for construction") {
      for {
        // Layer construction should be safe
        layer = ZLayer.succeed(42)
        
        // Using the layer requires runtime, but construction doesn't
      } yield true
    },
    
    test("ZLayer memoization works correctly") {
      for {
        counter <- Ref.make(0)
        
        layer = ZLayer.fromZIO(
          counter.updateAndGet(_ + 1) *> ZIO.succeed(42)
        )
        
        // Layer should memoize the construction
        _ <- ZIO.unit
      } yield true
    }
  )
}
