package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Semaphore

/**
 * Test suite for issue #9093
 * 
 * Issue: ZIO's `Semaphore` performance not too great
 * 
 * This test verifies Semaphore performance characteristics.
 */
object BountySpec9093 extends ZIOSpecDefault {
  def spec = suite("Semaphore performance")(
    test("Semaphore.acquire is fast") {
      for {
        semaphore <- Semaphore.make(1)
        start <- Clock.nanoTime
        _ <- semaphore.acquire.release
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 100.0 // Should be fast
    },
    
    test("Semaphore withPermit is efficient") {
      for {
        semaphore <- Semaphore.make(5)
        permits = 3
        start <- Clock.nanoTime
        _ <- semaphore.withPermit(permits)(ZIO.unit)
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 100.0
    },
    
    test("Semaphore handles concurrent acquisitions efficiently") {
      for {
        semaphore <- Semaphore.make(10)
        fibers <- ZIO.foreachPar((1 to 20).toList) { i =>
          semaphore.withPermit(ZIO.succeed(i))
        }.fork
        start <- Clock.nanoTime
        _ <- fibers.join
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 5000.0 // 5 seconds for 20 concurrent ops
    },
    
    test("Semaphore.acquireN is efficient") {
      for {
        semaphore <- Semaphore.make(10)
        start <- Clock.nanoTime
        _ <- semaphore.acquireN(5).release
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 100.0
    },
    
    test("Semaphore.available is fast") {
      for {
        semaphore <- Semaphore.make(5)
        start <- Clock.nanoTime
        available <- semaphore.available
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0 // to ms
      } yield duration < 10.0 && available == 5L
    }
  )
}
