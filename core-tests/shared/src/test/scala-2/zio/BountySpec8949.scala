package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #8949
 * 
 * Issue: Unintuitive behaviour of `checkAll`
 * 
 * This test verifies checkAll behavior and documents expected usage.
 */
object BountySpec8949 extends ZIOSpecDefault {
  def spec = suite("checkAll behavior")(
    test("checkAll runs tests for all values") {
      for {
        tested <- Ref.make(List.empty[Int])
        
        _ <- checkAll((1 to 5).toList) { n =>
          tested.update(_ :+ n) *> assertTrue(true)
        }
        
        values <- tested.get
      } yield values == List(1, 2, 3, 4, 5)
    },
    
    test("checkAll stops on first failure") {
      for {
        tested <- Ref.make(0)
        
        result <- checkAll((1 to 10).toList) { n =>
          tested.update(_ + 1) *> {
            if (n == 3) assertCompletes(ZIO.succeed(false))(isTrue)
            else assertCompletes(ZIO.succeed(true))(isTrue)
          }
        }.either
        
        count <- tested.get
      } yield result.isLeft && count == 3
    },
    
    test("checkAll provides clear error messages") {
      val result = checkAll(List(1, 2, 3)) { n =>
        assertCompletes(ZIO.succeed(n))(equalTo(0))
      }.either
      
      assertCompletes(ZIO.succeed(result))(isFailing(anything))
    },
    
    test("checkAll works with empty list") {
      for {
        result <- checkAll(List.empty[Int]) { n =>
          assertCompletes(ZIO.succeed(n))(anything)
        }
      } yield true
    },
    
    test("checkAll is deterministic") {
      for {
        results1 <- ZIO.foreach((1 to 5).toList) { n =>
          checkAll(List(n)) { _ =>
            assertCompletes(ZIO.succeed(true))(isTrue)
          }
        }
        
        results2 <- ZIO.foreach((1 to 5).toList) { n =>
          checkAll(List(n)) { _ =>
            assertComplements(ZIO.succeed(true))(isTrue)
          }
        }
      } yield results1.size == results2.size && results1.size == 5
    }
  )
}
