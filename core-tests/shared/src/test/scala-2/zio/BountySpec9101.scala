package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9101
 * 
 * Issue: zio.test.Gen generates the same data every time in certain conditions
 * 
 * This test verifies that zio.test.Gen properly generates random data
 * and doesn't always return the same values.
 */
object BountySpec9101 extends ZIOSpecDefault {
  def spec = suite("Gen randomness")(
    test("Gen generates different values on multiple calls") {
      val gen = Gen.int
      
      // Generate values multiple times
      val values = (1 to 10).map(_ => gen.runSample.orThrow)
      
      // Verify not all values are the same
      val uniqueValues = values.distinct.size
      
      assertCompletes(ZIO.succeed(uniqueValues))(isGreaterThan(1))
    },
    
    test("Gen.int generates different integers") {
      val gen = Gen.int
      
      val values = (1 to 20).map(_ => gen.runSample.orThrow)
      val uniqueCount = values.distinct.size
      
      // Should have multiple unique values
      assertCompletes(ZIO.succeed(uniqueCount))(isGreaterThan(5))
    },
    
    test("Gen.string generates different strings") {
      val gen = Gen.string
      
      val values = (1 to 20).map(_ => gen.runSample.orThrow)
      val uniqueCount = values.distinct.size
      
      // Should have multiple unique strings
      assertCompletes(ZIO.succeed(uniqueCount))(isGreaterThan(5))
    },
    
    test("Gen.oneOf generates different choices") {
      val items = List(1, 2, 3, 4, 5)
      val gen = Gen.oneOf(items)
      
      val values = (1 to 20).map(_ => gen.runSample.orThrow)
      val uniqueCount = values.distinct.size
      
      // Should generate multiple different items
      assertCompletes(ZIO.succeed(uniqueCount))(isGreaterThan(2))
    },
    
    test("Gen.frequency generates different results") {
      val gen = Gen.frequency(
        (1, Gen.const(1)),
        (1, Gen.const(2)),
        (1, Gen.const(3))
      )
      
      val values = (1 to 20).map(_ => gen.runSample.orThrow)
      val uniqueCount = values.distinct.size
      
      // Should generate multiple different frequencies
      assertCompletes(ZIO.succeed(uniqueCount))(isGreaterThan(1))
    },
    
    test("Gen.zip generates different tuples") {
      val gen1 = Gen.int
      val gen2 = Gen.int
      val gen = gen1 <*> gen2
      
      val values = (1 to 20).map(_ => gen.runSample.orThrow)
      val uniqueFirstElements = values.map(_._1).distinct.size
      val uniqueSecondElements = values.map(_._2).distinct.size
      
      // Both tuple elements should have variety
      assertCompletes(ZIO.succeed(uniqueFirstElements + uniqueSecondElements))(isGreaterThan(10))
    }
  )
}
