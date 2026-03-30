# ZIO Performance Optimization Guide

## Overview

This guide provides practical tips for optimizing ZIO applications for maximum performance.

## 1. Concurrency & Parallelism

### Use `foreachPar` for Independent Operations

```scala
// ❌ Sequential
val results = ZIO.foreach(items)(processItem)

// ✅ Parallel
val results = ZIO.foreachPar(items)(processItem)
```

### Limit Parallelism

```scala
// Control concurrency
ZIO.foreachPar(items)(processItem)
  .withParallelism(10) // Max 10 concurrent operations
```

### Use `mapZIO` vs `mapZIOPar`

```scala
// Sequential transformation
stream.mapZIO(processElement)

// Parallel transformation (higher throughput)
stream.mapZIOPar(10)(processElement)
```

## 2. Stream Optimization

### Batch Processing

```scala
// Process elements in batches
stream.grouped(100).foreachZIO { batch =>
  // Process batch efficiently
}
```

### Buffer Management

```scala
// Use bufferStrict for precise backpressure
stream.bufferStrict(100)

// Use buffer for higher throughput
stream.buffer(100)
```

### Avoid Unnecessary Operations

```scala
// ❌ Multiple passes
stream.filter(isValid)
     .map(transform)
     .filter(isValid)

// ✅ Single pass
stream.filter(isValid)
     .mapZIO(elem => transform(elem).flatMap(validate))
```

## 3. Fiber Management

### Avoid Excessive Forking

```scala
// ❌ Fork for each item
ZIO.foreach(items)(item => process(item).fork)

// ✅ Use foreachPar
ZIO.foreachPar(items)(process(item))
```

### Use Fiber Pools

```scala
// Limit concurrent fibers
val semaphore = Semaphore.make(100)

ZIO.foreachPar(items) { item =>
  semaphore.withPermit(process(item))
}
```

## 4. Resource Management

### Reuse Resources

```scala
// ❌ Create connection each time
def query(sql: String) = for {
  conn <- acquireConnection
  result <- execute(conn, sql)
  _     <- releaseConnection(conn)
} yield result

// ✅ Use connection pool
def query(sql: String) = connectionPool.use { conn =>
  execute(conn, sql)
}
```

### Use `Scoped` Resources

```scala
// Automatic cleanup
ZIO.scoped {
  for {
    client <- HttpClient.scoped
    result <- client.request(request)
  } yield result
}
```

## 5. Error Handling

### Avoid Catch-All Error Handling

```scala
// ❌ Catch all errors
effect.catchAll(_ => ZIO.succeed(default))

// ✅ Handle specific errors
effect.catchSome { case _: SpecificError =>
  ZIO.succeed(default)
}
```

### Use `retry` for Transient Failures

```scala
// Retry with backoff
effect.retry(Schedule.exponentialBackoff(100.millis))
```

## 6. Memory Management

### Use Chunked Operations

```scala
// Process in chunks to avoid memory spikes
stream.chunks.foreachZIO { chunk =>
  processChunk(chunk)
}
```

### Clear Periodically

```scala
// For long-running processes
ZIO.foreachDiscard(List.range(0, 100)) { i =>
  process *> ZIO.when(i % 1000 == 0)(clearCache)
}
```

## 7. Scheduler Configuration

### Use Appropriate Executor

```scala
// CPU-bound operations
val cpuExecutor = Executor.fromExecutionContext(
  cpuEC,
  "cpu-bound"
)

// IO-bound operations
val ioExecutor = Executor.fromExecutionContext(
  ioEC,
  "io-bound"
)
```

### Tune Scheduler Size

```scala
// Match executor size to workload
val executor = Executor.fromThreadPool(
  corePoolSize = Runtime.getRuntime.availableProcessors(),
  maxPoolSize = Runtime.getRuntime.availableProcessors() * 2
)
```

## 8. Monitoring & Profiling

### Use Runtime Metrics

```scala
// Enable runtime metrics
runtime.provideLayer(Runtime.enableRuntimeMetrics)

// Access metrics
for {
  metrics <- ZIO.runtimeMetrics
  _      <- ZIO.logInfo(s"Fiber count: ${metrics.fiberCount}")
} yield ()
```

### Profile Your Application

```scala
// Use operation logging
runtime.provideLayer(Runtime.enableOpLog)

// Analyze logs
ZRuntimeOpLog.printLogs
```

## 9. Compilation & Build

### Use Scalac Options

```scala
// scalac options for performance
scalacOptions ++= Seq(
  "-opt:l:inline", // Enable inlining
  "-opt-inline-from:**" // Inline from all sources
)
```

### Avoid Reflection

```scala
// ❌ Runtime reflection
val method = clazz.getMethod("process")
method.invoke(instance)

// ✅ Compile-time
instance.process
```

## 10. Testing Performance

### Benchmark Critical Paths

```scala
import zio.test._

object PerformanceSpec extends ZIOBaseSpec {
  def spec = suite("Performance")(
    test("process 1000 items in < 1s") {
      for {
        start <- Clock.nanoTime
        _     <- processItems(1000)
        end   <- Clock.nanoTime
        duration = (end - start) / 1000000 // to ms
      } yield assertTrue(duration < 1000)
    }
  )
}
```

### Load Testing

```scala
// Simulate concurrent load
val loadTest = ZIO.foreachPar(1000) { _ =>
  apiCall
}
```

## Common Pitfalls

### ❌ Don't

- Block threads in ZIO effects
- Use `unsafeRun` in application code
- Create unnecessary layers
- Ignore backpressure
- Excessive synchronization

### ✅ Do

- Use asynchronous operations
- Leverage ZIO's concurrency primitives
- Profile before optimizing
- Use appropriate data structures
- Monitor memory usage

## Further Reading

- [ZIO Performance Guide](https://zio.dev/performance)
- [ZStream Performance](https://zio.dev/guide/stream/overview#performance)
- [Executor Configuration](https://zio.dev/executors)

## Conclusion

Performance optimization is an iterative process:
1. Profile first
2. Identify bottlenecks
3. Apply targeted optimizations
4. Measure improvements
5. Repeat

Always measure the impact of optimizations!
