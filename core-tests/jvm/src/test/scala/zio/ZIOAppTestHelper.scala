package zio

import zio._
import java.lang.{Process => JProcess, ProcessBuilder => JProcessBuilder}
import java.io.{BufferedReader, File, InputStreamReader}
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

/**
 * Helper utilities for testing ZIOApp behavior in subprocesses.
 *
 * This provides the infrastructure needed to test signal handling,
 * exit codes, finalizer execution, and shutdown behavior.
 */
object ZIOAppTestHelper {

  /**
   * Configuration for running a subprocess test.
   */
  final case class SubprocessConfig(
    mainClass: String,
    classPath: List[String],
    args: List[String] = Nil,
    jvmArgs: List[String] = Nil,
    timeout: Duration = 10.seconds
  )

  /**
   * Result of running a subprocess.
   */
  final case class SubprocessResult(
    exitCode: Int,
    stdout: String,
    stderr: String,
    executionTime: Duration
  )

  /**
   * Runs a ZIOApp as a subprocess and captures its output and exit code.
   *
   * @param config Configuration for the subprocess
   * @return ZIO effect that produces the subprocess result
   */
  def runSubprocess(config: SubprocessConfig): ZIO[Any, Throwable, SubprocessResult] =
    ZIO.attemptBlocking {
      val startTime = System.currentTimeMillis()

      // Build the process command
      val javaExec = System.getProperty("java.home") + "/bin/java"
      val cp = config.classPath.mkString(File.pathSeparator)
      val cmd = List(javaExec) ++
        config.jvmArgs ++
        List("-cp", cp, config.mainClass) ++
        config.args

      // Start the process
      val builder = new JProcessBuilder(cmd.asJava)
      builder.redirectErrorStream(false)

      val process = builder.start()

      // Read streams in separate threads to avoid blocking
      val stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream))
      val stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream))

      val stdoutBuilder = new StringBuilder()
      val stderrBuilder = new StringBuilder()

      // Threads to read output
      val stdoutThread = new Thread(() => {
        var line: String = null
        while ({ line = stdoutReader.readLine(); line != null }) {
          stdoutBuilder.append(line).append("\n")
        }
      })

      val stderrThread = new Thread(() => {
        var line: String = null
        while ({ line = stderrReader.readLine(); line != null }) {
          stderrBuilder.append(line).append("\n")
        }
      })

      stdoutThread.start()
      stderrThread.start()

      // Wait for process to complete or timeout
      val timeoutMs = config.timeout.toMillis
      val exited = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)

      if (!exited) {
        // Timeout - destroy the process
        process.destroyForcibly()
      }

      // Wait for readers to finish
      stdoutThread.join(1000)
      stderrThread.join(1000)

      val endTime = System.currentTimeMillis()
      val executionTime = (endTime - startTime).millis

      SubprocessResult(
        exitCode = if (exited) process.exitValue() else -1,
        stdout = stdoutBuilder.toString,
        stderr = stderrBuilder.toString,
        executionTime = executionTime
      )
    }

  /**
   * Sends a signal to a running process by PID.
   *
   * @param pid Process ID
   * @param signalName Signal name (e.g., "INT", "TERM")
   * @return ZIO effect that sends the signal
   */
  def sendSignal(pid: Long, signalName: String): ZIO[Any, Throwable, Unit] =
    ZIO.attemptBlocking {
      val runtime = Runtime.getRuntime
      signalName.toLowerCase match {
        case "int" | "sigint" =>
          // On Unix-like systems, send SIGINT
          if (!System.getProperty("os.name").toLowerCase.contains("windows")) {
            val killProcess = runtime.exec(Array("kill", "-INT", pid.toString))
            killProcess.waitFor()
          } else {
            // On Windows, we can't send SIGINT easily
            // For testing purposes, we might skip this on Windows
            throw new UnsupportedOperationException("SIGINT not supported on Windows")
          }

        case "term" | "sigterm" =>
          if (!System.getProperty("os.name").toLowerCase.contains("windows")) {
            val killProcess = runtime.exec(Array("kill", "-TERM", pid.toString))
            killProcess.waitFor()
          } else {
            throw new UnsupportedOperationException("SIGTERM not supported on Windows")
          }

        case _ =>
          throw new IllegalArgumentException(s"Unknown signal: $signalName")
      }
    }.catchSome {
      case _: UnsupportedOperationException =>
        // On platforms that don't support signals, just log and continue
        ZIO.unit
    }

  /**
   * Finds the process ID of a Java process by main class name.
   *
   * @param mainClass Main class name to search for
   * @return ZIO effect that produces the PID, if found
   */
  def findPidByMainClass(mainClass: String): ZIO[Any, Throwable, Option[Long]] =
    ZIO.attemptBlocking {
      val runtime = Runtime.getRuntime
      val jpsProcess = runtime.exec(Array("jps", "-l"))
      val output = scala.io.Source.fromInputStream(jpsProcess.getInputStream).mkString
      jpsProcess.waitFor()

      output.linesIterator
        .find(_.contains(mainClass))
        .map { line =>
          val parts = line.split(" ")
          parts(0).toLong
        }
    }.catchSome {
      case _: Throwable => ZIO.none
    }
}
