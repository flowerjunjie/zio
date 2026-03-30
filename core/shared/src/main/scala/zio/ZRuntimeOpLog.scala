package zio

import zio.OpLog._

/**
 * Runtime Operation Logging API - #9170
 *
 * Provides methods to access and query operation logs.
 */
object ZRuntimeOpLog {

  /**
   * Get all operation logs for the current fiber.
   */
  def getLogs(implicit trace: Trace): ZIO[Any, Nothing, List[OpLogEntry]] =
    ZIO.succeed {
      val fiberId = ZIO.fiberId
      OpLog.getStore.getLogs(fiberId)
    }

  /**
   * Get all operation logs across all fibers.
   */
  def getAllLogs(implicit trace: Trace): ZIO[Any, Nothing, Map[FiberId, List[OpLogEntry]]] =
    ZIO.succeed(OpLog.getStore.getAllLogs)

  /**
   * Get operation logs for a specific fiber.
   */
  def getLogsFor(fiberId: FiberId)(implicit trace: Trace): ZIO[Any, Nothing, List[OpLogEntry]] =
    ZIO.succeed(OpLog.getStore.getLogs(fiberId))

  /**
   * Clear all operation logs.
   */
  def clearLogs(implicit trace: Trace): ZIO[Any, Nothing, Unit] =
    ZIO.succeed(OpLog.getStore.clear())

  /**
   * Get the number of fibers with logged operations.
   */
  def getLogCount(implicit trace: Trace): ZIO[Any, Nothing, Int] =
    ZIO.succeed(OpLog.getStore.size)

  /**
   * Print operation logs for debugging.
   */
  def printLogs(implicit trace: Trace): ZIO[Any, Nothing, Unit] =
    for {
      logs <- getAllLogs
      _    <- ZIO.foreachDiscard(logs) { case (fiberId, entries) =>
        ZIO.logInfo(s"Fiber $fiberId operations:") *>
        ZIO.foreachDiscard(entries) { entry =>
          ZIO.logInfo(s"  ${formatEntry(entry)}")
        }
      }
    } yield ()

  private def formatEntry(entry: OpLogEntry): String = {
    val micros = entry.timestamp / 1000
    entry match {
      case OpLog.FiberCreated(_, _, parent) =>
        s"[${micros}μs] Created (parent: ${parent.getOrElse("none")})"
      case OpLog.FiberTerminated(_, _, exit) =>
        s"[${micros}μs] Terminated with $exit"
      case OpLog.OperationExecuted(_, _, op, duration) =>
        s"[${micros}μs] $op (${duration / 1000}μs)"
      case OpLog.AsyncStarted(_, _, asyncId) =>
        s"[${micros}μs] Async started: $asyncId"
      case OpLog.AsyncCompleted(_, _, asyncId) =>
        s"[${micros}μs] Async completed: $asyncId"
    }
  }
}
