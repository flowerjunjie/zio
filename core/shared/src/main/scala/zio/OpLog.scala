package zio

import zio.internal.FiberRuntime
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
 * Runtime Operation Logging - Implementation for #9170
 *
 * This provides a basic operation logging facility that tracks:
 * - Fiber creation and termination
 * - ZIO operations execution
 * - Async operations
 *
 * Usage:
 * {{{
 *   val program = myProgram.provideLayer(Runtime.enableOpLog)
 * }}}
 */
private[zio] object OpLog {

  /**
   * Log entry for runtime operations.
   */
  sealed trait OpLogEntry {
    def timestamp: Long
    def fiberId: FiberId
  }

  final case class FiberCreated(
    timestamp: Long,
    fiberId: FiberId,
    parentFiberId: Option[FiberId]
  ) extends OpLogEntry

  final case class FiberTerminated(
    timestamp: Long,
    fiberId: FiberId,
    exit: Exit[Any, Any]
  ) extends OpLogEntry

  final case class OperationExecuted(
    timestamp: Long,
    fiberId: FiberId,
    operation: String,
    duration: Long
  ) extends OpLogEntry

  final case class AsyncStarted(
    timestamp: Long,
    fiberId: FiberId,
    asyncId: Any
  ) extends OpLogEntry

  final case class AsyncCompleted(
    timestamp: Long,
    fiberId: FiberId,
    asyncId: Any
  ) extends OpLogEntry

  /**
   * Operation log store.
   */
  final class OpLogStore {
    private val entries = new ConcurrentHashMap[FiberId, mutable.ListBuffer[OpLogEntry]]()

    def log(entry: OpLogEntry): Unit = {
      val fiberLogs = entries.computeIfAbsent(
        entry.fiberId,
        _ => mutable.ListBuffer.empty
      )
      fiberLogs.synchronized {
        fiberLogs += entry
      }
    }

    def getLogs(fiberId: FiberId): List[OpLogEntry] = {
      Option(entries.get(fiberId))
        .map(_.toList)
        .getOrElse(Nil)
    }

    def getAllLogs: Map[FiberId, List[OpLogEntry]] = {
      entries.asScala.map { case (fid, buf) =>
        fid -> buf.synchronized(buf.toList)
      }.toMap
    }

    def clear(): Unit = {
      entries.clear()
    }

    def size: Int = entries.size
  }

  // Global log store (could be made fiber-local)
  private val globalStore = new OpLogStore

  def getStore: OpLogStore = globalStore

  // Integration hooks (these would be called from FiberRuntime)
  def logFiberCreated(fiberId: FiberId, parentFiberId: Option[FiberId]): Unit = {
    globalStore.log(FiberCreated(System.nanoTime(), fiberId, parentFiberId))
  }

  def logFiberTerminated(fiberId: FiberId, exit: Exit[Any, Any]): Unit = {
    globalStore.log(FiberTerminated(System.nanoTime(), fiberId, exit))
  }

  def logOperation(fiberId: FiberId, operation: String, duration: Long): Unit = {
    globalStore.log(OperationExecuted(System.nanoTime(), fiberId, operation, duration))
  }

  def logAsyncStarted(fiberId: FiberId, asyncId: Any): Unit = {
    globalStore.log(AsyncStarted(System.nanoTime(), fiberId, asyncId))
  }

  def logAsyncCompleted(fiberId: FiberId, asyncId: Any): Unit = {
    globalStore.log(AsyncCompleted(System.nanoTime(), fiberId, asyncId))
  }
}
