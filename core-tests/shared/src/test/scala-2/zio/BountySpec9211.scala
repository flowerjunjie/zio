package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9211
 * 
 * Issue: ZIO.timeoutTO has tremendous overhead
 * 
 * This test measures and tracks timeout overhead.
 */
object BountySpec9211 extends ZIOSpecDefault {
  def spec = suite("ZIO.timeoutTO overhead")(
    test("timeoutTO overhead is acceptable") {
      for {
        start <- Clock.nanoTime
        
        // Measure timeout overhead
        _ <- ZIO.succeed(42).timeout(1.second)
        
        end <- Clock.nanoTime
        durationNanos = end - start
        durationMicros = durationNanos / 1000.0
      } yield durationMicros < 10000.0 // Less than 10ms overhead
    },
    
    test("timeoutTO on fast effect has low overhead") {
      for {
        start <- Clock.nanoTime
        _ <- ZIO.unit.timeout(1.second)
        end <- Clock.nanoTime
        durationNanos = end - start
        durationMicros = durationNanos / 1000.0
      } yield durationMicros < 5000.0 // Less than 5ms for unit
    },
    
    test("timeoutTO without timeout has minimal overhead") {
      for {
        start <- Clock.nanoTime
        result <- ZIO.succeed(42).timeout(1.second)
        end <- Clock.nanoTime
        durationNanos = end - start
        durationMicros = durationNanos / 1000.0
        
        hasValue = result.isDefined
        overhead = durationMicros
      } yield hasValue && overhead < 10000.0
    },
    
    test("timeoutTO with actual timeout has correct behavior") {
      for {
        start <- Clock.nanoTime
        result <- ZIO.sleep(5.seconds).timeout(100.millis)
        end <- Clock.nanoTime
        durationNanos = end - start
        durationMicros = durationNanos / 1000.0
        
        timedOut = result.isEmpty
        duration = durationMicros
      } yield timedOut && duration >= 100000.0 && duration < 500000.0
    },
    
    test("nested timeoutTO has acceptable overhead") {
      for {
        start <- Clock.nanoTime
        result <- ZIO.succeed(42)
          .timeout(1.second)
          .flatMap(r => ZIO.succeed(r).timeout(1.second))
        end <- Clock.nanoTime
        durationNanos = end - start
        durationMicros = durationNanos / 1000.0
      } yield durationMicros < 20000.0 // Less than 20ms for nested
    }
  )
}
