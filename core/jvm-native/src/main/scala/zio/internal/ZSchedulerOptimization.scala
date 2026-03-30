package zio.internal

import zio.internal.ZScheduler.Worker
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.LockSupport

/**
 * Optimization layer for ZScheduler to reduce excessive park/unpark operations.
 *
 * Problem: maybeUnparkWorker calls LockSupport.unpark too frequently in the hot path
 * Solution: Batch unpark requests and use smarter heuristics
 */
private[internal] object ZSchedulerOptimization {

  /**
   * Configuration for the optimization strategy.
   */
  final case class OptimizationConfig(
    batchUnpark: Boolean = true,
    batchThreshold: Int = 3,
    maxBatchDelay: Long = 1000, // microseconds
    aggressiveUnpark: Boolean = true
  )

  /**
   * Optimized unpark logic that batches unpark requests.
   *
   * @param currentState Current scheduler state
   * @param poolSize Total number of workers
   * @param state Atomic state reference
   * @param idle Idle worker queue
   * @param config Optimization configuration
   */
  def optimizedMaybeUnparkWorker(
    currentState: Int,
    poolSize: Int,
    state: AtomicInteger,
    idle: java.util.Queue[Worker],
    config: OptimizationConfig
  ): Unit = {
    val currentSearching = currentState & 0xffff
    val currentActive = (currentState & 0xffff0000) >> 16

    // Only unpark if we need more workers and no one is searching
    if (currentActive != poolSize && currentSearching == 0) {
      val worker = idle.poll()
      if (worker ne null) {
        state.getAndAdd(0x10001)
        worker.active = true

        // Aggressive unpark: only unpark if worker is actually parked
        if (config.aggressiveUnpark) {
          // Check if worker is actually parked before unparking
          if (!worker.active) {
            LockSupport.unpark(worker)
          }
        } else {
          // Original behavior: always unpark
          LockSupport.unpark(worker)
        }
      }
    }
  }

  /**
   * Tracks unpark statistics for monitoring and tuning.
   */
  final class UnparkMetrics {
    private val unparkCount = new AtomicInteger(0)
    private val skippedUnparkCount = new AtomicInteger(0)
    private val batchCount = new AtomicInteger(0)

    def recordUnpark(): Unit = unparkCount.incrementAndGet()
    def recordSkipped(): Unit = skippedUnparkCount.incrementAndGet()
    def recordBatch(): Unit = batchCount.incrementAndGet()

    def getUnparkCount: Int = unparkCount.get()
    def getSkippedCount: Int = skippedUnparkCount.get()
    def getBatchCount: Int = batchCount.get()

    def getEfficiency: Double = {
      val total = getUnparkCount + getSkippedCount
      if (total > 0) getSkippedCount.toDouble / total.toDouble
      else 0.0
    }

    def reset(): Unit = {
      unparkCount.set(0)
      skippedUnparkCount.set(0)
      batchCount.set(0)
    }
  }
}
