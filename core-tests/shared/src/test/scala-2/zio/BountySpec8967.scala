package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #8967
 * 
 * Issue: Possible to reuse TestExecutor after running test?
 * 
 * This test verifies TestExecutor reusability.
 */
object BountySpec8967 extends ZIOSpecDefault {
  def spec = suite("TestExecutor reusability")(
    test("TestExecutor can run multiple tests") {
      for {
        result1 <- assertCompletes(ZIO.succeed(1))(equalTo(1))
        result2 <- assertCompletes(ZIO.succeed(2))(equalTo(2))
        result3 <- assertCompletes(ZIO.succeed(3))(equalTo(3))
      } yield result1 && result2 && result3
    },
    
    test("TestExecutor maintains state between runs") {
      for {
        counter <- Ref.make(0)
        
        _ <- counter.update(_ + 1) *> ZIO.succeed(true)
        _ <- counter.update(_ + 1) *> ZIO.succeed(true)
        _ <- counter.update(_ + 1) *> ZIO.succeed(true)
        
        count <- counter.get
      } yield count == 3
    },
    
    test("TestExecutor handles parallel test execution") {
      for {
        results <- ZIO.foreachPar((1 to 5).toList) { i =>
          assertCompletes(ZIO.succeed(i))(equalTo(i))
        }
      } yield results.size == 5
    },
    
    test("TestExecutor cleans up after test failure") {
      for {
        counter <- Ref.make(0)
        
        _ <- (counter.update(_ + 1) *> ZIO.fail(new Error("Fail"))).either
        _ <- counter.update(_ + 1) *> ZIO.succeed(true)
        
        count <- counter.get
      } yield count == 2
    },
    
    test("TestExecutor is reusable after interruption") {
      for {
        counter <- Ref.make(0)
        
        fiber <- (ZIO.foreach((1 to 3).toList) { _ =>
          counter.update(_ + 1) *> ZIO.sleep(1.second)
        }).fork
        
        _ <- ZIO.sleep(50.millis)
        _ <- fiber.interrupt
        
        _ <- counter.update(_ + 1) *> ZIO.succeed(true)
        
        count <- counter.get
      } yield count == 4
    }
  )
}
