# ZStream.fromInputStreamInterruptible

## Overview
`ZStream.fromInputStreamInterruptible` creates a stream from an `InputStream` that supports interruption, allowing for better resource management and responsiveness.

## Motivation
The existing `ZStream.fromInputStream` doesn't properly handle interruption, which can lead to:
- Resource leaks when streams are interrupted
- Hanging fibers during shutdown
- Inability to cancel long-running stream operations

## API Design
```scala
object ZStream {
  def fromInputStreamInterruptible(
    is: => InputStream,
    chunkSize: Int = 8192
  ): ZStream[Byte] = ???
}
```

## Usage Example
```scala
import zio._
import java.io._

val fileStream = ZStream.fromInputStreamInterruptible(
  new FileInputStream("large-file.dat"),
  chunkSize = 8192
)

fileStream
  .runDrain
  .catchAll(e => ZIO.logError(s"Stream failed: $e"))
```

## Behavior
| Operation | Interruptible? |
|------------|---------------|
| Stream creation | Yes |
| Read operations | Yes |
| Cleanup/close | Yes |
| Error handling | Yes |

## Implementation Notes
- Uses bracket pattern for resource safety
- Propagates interruption signals to the underlying stream
- Ensures InputStream is properly closed on interruption
- Chunk size can be tuned for performance

## Use Cases
1. **File Processing**: Interruptible file reads with proper cleanup
2. **Network Streams**: Cancelable HTTP response streaming
3. **Large Files**: Progressive processing with cancellation support
4. **Resource-constrained**: Clean shutdown when memory is limited

## Related
- Issue: #9084
- Existing: ZStream.fromInputStream
- Pattern: ZStream.bracket

## Example: Comparison
```scala
// Non-interruptible (existing)
val stream1 = ZStream.fromInputStream(inputStream)
// May hang if interrupted!

// Interruptible (proposed)
val stream2 = ZStream.fromInputStreamInterruptible(inputStream)
// Properly handles interruption!
```
