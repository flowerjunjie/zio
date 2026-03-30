# ZStream.buffer Off-by-One Fix (#9810)

## Problem Statement

When using `buffer(1)`, users expect only 1 element to be buffered, but actually 2 elements are buffered:
- Element 1: Being processed by consumer
- Element 2: Already in queue, waiting to be processed

## Root Cause

The current implementation uses `Queue.bounded(capacity)`, which allows:
1. Consumer takes element 1 from queue (now processing)
2. Queue has 0 elements (has space)
3. Producer immediately adds element 2
4. Result: 2 elements in-flight (1 processing + 1 queued)

## Solution

We need to clarify the semantics of `buffer(n)`:

**Current semantics (confusing):**
- `buffer(n)` creates a queue of size n
- This allows n+1 elements in-flight (n in queue + 1 being processed)

**Proposed semantics (clear):**
- `buffer(n)` allows at most n elements in-flight
- This means queue size should be n-1 (to account for the element being processed)

## Implementation

The fix is to adjust the queue size when creating the buffer:

```scala
def buffer(capacity: => Int)(implicit trace: Trace): ZStream[R, E, A] = {
  // Fix: Use capacity-1 for queue size to account for element being processed
  val queueCapacity = math.max(1, capacity) // Ensure at least 1
  val queue = self.toQueueOfElements(queueCapacity)
  // ... rest of implementation
}
```

However, this is a breaking change. Better approach:

**Option 1: Add new method with clear semantics**
```scala
def bufferStrict(capacity: => Int): ZStream[R, E, A]
```

**Option 2: Fix with deprecation**
```scala
@deprecated("Use bufferSliding for old behavior, buffer for new strict behavior", "2.2.0")
def buffer(capacity: => Int): ZStream[R, E, A] = bufferStrict(capacity)
```

**Option 3: Document and accept current behavior**
Update documentation to clarify that buffer(n) allows n+1 in-flight elements.

## Recommendation

Given the potential impact on existing code, I recommend:
1. Add `bufferStrict` method with correct semantics
2. Document current `buffer` behavior clearly
3. Deprecate `buffer` in favor of `bufferStrict` in next major version

This allows users to choose the behavior they want while maintaining backward compatibility.
