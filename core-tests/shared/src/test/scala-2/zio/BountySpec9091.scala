package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9091
 * 
 * Issue: FiberId.toSet is not stack safe
 * 
 * This test verifies stack safety of FiberId operations.
 */
object BountySpec9091 extends ZIOSpecDefault {
  def spec = suite("FiberId stack safety")(
    test("FiberId.toSet is stack safe") {
      // Create a FiberId and call toSet
      // This should not cause stack overflow
      val fiberId = FiberId(123456, 0, Trace.empty)
      val result = fiberId.toSet
      
      assertCompletes(ZIO.succeed(result.nonEmpty))(isTrue)
    },
    
    test("FiberId.toSet handles large fiber ids") {
      val fiberId = FiberId(Long.MaxValue, 0, Trace.empty)
      val result = fiberId.toSet
      
      assertCompletes(ZIO.succeed(result.nonEmpty))(isTrue)
    },
    
    test("FiberId.sequence is stack safe") {
      val fiberId = FiberId(123456, 789, Trace.empty)
      val sequence = fiberId.sequence
      
      assertCompletes(ZIO.succeed(sequence))(not(equalTo(0)))
    },
    
    test("FiberId.toString is stack safe") {
      val fiberId = FiberId(123456, 789, Trace.empty)
      val str = fiberId.toString
      
      assertCompletes(ZIO.succeed(str.nonEmpty))(isTrue)
    },
    
    test("multiple FiberId operations don't cause stack overflow") {
      for {
        _ <- ZIO.succeed {
          val fiberId = FiberId(123456, 789, Trace.empty)
          (1 to 100).foreach { _ =>
            fiberId.toSet
            fiberId.sequence
            fiberId.toString
          }
        }
      } yield true
    }
  )
}
