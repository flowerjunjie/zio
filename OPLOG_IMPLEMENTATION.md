# Runtime Operation Logging - Implementation Guide (#9170)

## Overview

This document describes the implementation of Runtime Operation Logging, which provides visibility into ZIO runtime operations for debugging and monitoring.

## Status

Previously, `Runtime.enableOpLog` was defined but not implemented. This implementation adds a basic operation logging facility.

## Features

### What Gets Logged

1. **Fiber Lifecycle**
   - Fiber creation (with parent reference)
   - Fiber termination (with exit value)

2. **Operations**
   - ZIO operation execution
   - Operation timing

3. **Async Operations**
   - Async operation start
   - Async operation completion

### Usage

Enable operation logging:

```scala
import zio._

val program = for {
  _ <- ZIO.logInfo("Hello")
  _ <- ZIO.sleep(1.second)
  _ <- ZIO.logInfo("World")
} yield ()

program.provideLayer(Runtime.enableOpLog)
```

Access logs programmatically:

```scala
import zio._

val program = for {
  // Your application logic
  _ <- myApp

  // Access logs
  logs <- ZRuntimeOpLog.getAllLogs
  _ <- ZIO.foreach(logs) { case (fiberId, entries) =>
    ZIO.logInfo(s"Fiber $fiberId: ${entries.length} operations")
  }
} yield ()
```

### API Reference

#### `ZRuntimeOpLog`

- `getLogs`: Get logs for current fiber
- `getAllLogs`: Get logs for all fibers
- `getLogsFor(fiberId)`: Get logs for specific fiber
- `clearLogs`: Clear all logs
- `getLogCount`: Get count of fibers with logs
- `printLogs`: Print all logs (for debugging)

## Implementation Details

### OpLogEntry Types

```scala
sealed trait OpLogEntry {
  def timestamp: Long
  def fiberId: FiberId
}

// Fiber lifecycle
FiberCreated(timestamp, fiberId, parentFiberId)
FiberTerminated(timestamp, fiberId, exit)

// Operations
OperationExecuted(timestamp, fiberId, operation, duration)

// Async operations
AsyncStarted(timestamp, fiberId, asyncId)
AsyncCompleted(timestamp, fiberId, asyncId)
```

### Storage

- Uses `ConcurrentHashMap` for thread-safe storage
- Per-fiber log buffers
- Global log store

### Performance Considerations

- Logging is designed to be low-overhead
- Logs are stored in memory
- Consider clearing logs periodically in long-running applications

## Limitations

1. **Memory Usage**: Logs accumulate in memory
   - Use `clearLogs` periodically
   - Consider sampling for high-volume scenarios

2. **Performance**: Minimal overhead when enabled
   - Timestamp generation
   - Concurrent map operations

3. **Completeness**: Not all operations are logged
   - Current implementation logs key lifecycle events
   - Can be extended for more detailed logging

## Future Enhancements

1. **Structured Logging**: Integration with existing logging
2. **Filtering**: Log only specific fibers or operations
3. **Sampling**: Statistical sampling for production
4. **Export**: Export logs to external systems
5. **Metrics**: Integration with RuntimeMetrics

## Related

- Issue: #9170
- RuntimeFlag: `RuntimeFlag.OpLog`
- Runtime Layer: `Runtime.enableOpLog`
