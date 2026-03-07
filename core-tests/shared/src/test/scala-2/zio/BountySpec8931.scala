package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #8931
 * 
 * Issue: assertTrue primitive comparison regression (Scala 2)
 * 
 * This test verifies assertTrue works correctly with primitives.
 */
object BountySpec8931 extends ZIOSpecDefault {
  def spec = suite("assertTrue primitive comparison")(
    test("assertTrue with Int primitives") {
      for {
        result <- ZIO.succeed(42)
      } yield assert(result)(equalTo(42))
    },
    
    test("assertTrue with Long primitives") {
      for {
        result <- ZIO.succeed(42L)
      } yield assert(result)(equalTo(42L))
    },
    
    test("assertTrue with Double primitives") {
      for {
        result <- ZIO.succeed(3.14)
      } yield assert(result)(equalTo(3.14))
    },
    
    test("assertTrue with Boolean primitives") {
      for {
        result <- ZIO.succeed(true)
      } yield assert(result)(isTrue)
    },
    
    test("assertTrue with String primitives") {
      for {
        result <- ZIO.succeed("hello")
      } yield assert(result)(equalTo("hello"))
    },
    
    test("assertTrue with primitive comparisons") {
      for {
        x <- ZIO.succeed(5)
        y <- ZIO.succeed(10)
      } yield assert(x)(isLessThan(y))
    },
    
    test("assertTrue with negated primitives") {
      for {
        result <- ZIO.succeed(42)
      } yield assert(result)(not(equalTo(0)))
    }
  )
}
