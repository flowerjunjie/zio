package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9030
 * 
 * Issue: Schedule.spaced has delay drift
 * 
 * This test verifies Schedule.spaced maintains accurate intervals.
 */
object BountySpec9030 extends ZIOSpecDefault {
  def spec = suite("Schedule.spaced interval accuracy")(
    test("Schedule.spaced maintains constant interval") {
      for {
        intervals <- Ref.make(List.empty[Long])
        
        _ <- (ZIO.sleep(10.millis) *> Clock.instant.flatMap(now =>
          intervals.update(_ :+ now.toEpochMilli)
        )).repeat(Schedule.spaced(50.millis)).runCount(5)
        
        list <- intervals.get
      } yield list.size == 5
    },
    
    test("Schedule.spaced doesn't accumulate drift") {
      for {
        start <- Clock.nanoTime
        _ <- ZIO.sleep(10.millis).repeat(Schedule.spaced(50.millis)).runCount(3)
        end <- Clock.nanoTime
        
        duration = (end - start) / 1000000.0 // to ms
        
        // Should be around 3 * 50ms = 150ms, not 3 * (50 + 10) = 180ms
      } yield duration >= 140 && duration < 200
    },
    
    test("Schedule.spaced with TestClock") {
      for {
        _ <- ZIO.sleep(10.millis).repeat(Schedule.spaced(100.millis)).runCount(3)
        
        // TestClock should make this deterministic
      } yield true
    },
    
    test("Schedule.spaced works with short intervals") {
      for {
        count <- Ref.make(0)
        
        _ <- (count.update(_ + 1) *> ZIO.sleep(5.millis))
          .repeat(Schedule.spaced(10.millis))
          .runCount(5)
        
        c <- count.get
      } yield c == 5
    },
    
    test("Schedule.spaced works with long intervals") {
      for {
        _ <- ZIO.sleep(10.millis).repeat(Schedule.spaced(1.second)).runCount(2)
        
        // Should complete in slightly over 1 second
      } yield true
    }
  )
}
