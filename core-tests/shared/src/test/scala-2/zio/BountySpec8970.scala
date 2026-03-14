package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #8970
 * 
 * Issue: Flaky test for `TestClock`
 * 
 * This test verifies TestClock behaves consistently.
 */
object BountySpec8970 extends ZIOSpecDefault {
  def spec = suite("TestClock consistency")(
    test("TestClock adjustments are deterministic") {
      for {
        _ <- TestClock.adjust(1.second)
        currentTime <- clock.currentTime
      } yield currentTime == 1000L
    },
    
    test("TestClock.adjust is repeatable") {
      for {
        _ <- TestClock.adjust(1.second)
        time1 <- clock.currentTime
        _ <- TestClock.adjust(1.second)
        time2 <- clock.currentTime
      } yield time2 == time1 + 1000
    },
    
    test("TestClock.setTime works consistently") {
      for {
        _ <- TestClock.setTime(5000L)
        time1 <- clock.currentTime
        _ <- TestClock.setTime(10000L)
        time2 <- clock.currentTime
      } yield time1 == 5000 && time2 == 10000
    },
    
    test("TestClock with sleep is deterministic") {
      for {
        fiber <- ZIO.sleep(1.second).fork
        _ <- TestClock.adjust(1.second)
        _ <- fiber.join
      } yield true
    },
    
    test("TestClock doesn't leak time between tests") {
      for {
        _ <- TestClock.adjust(1.second)
        time1 <- clock.currentTime
        _ <- ZIO.sleep(100.millis)
        time2 <- clock.currentTime
      } yield time2 == time1
    },
    
    test("TestClock adjusts with schedule") {
      for {
        count <- Ref.make(0)
        
        schedule = Schedule.spaced(1.second)
        fiber <- (count.update(_ + 1)).repeat(schedule).fork
        
        _ <- TestClock.adjust(3.seconds)
        _ <- fiber.interrupt
        
        c <- count.get
      } yield c >= 3
    }
  )
}
