# ZIO Error Handling Best Practices

## Overview

Error handling is a core strength of ZIO. This guide covers best practices for effective error management.

## 1. Error Types in ZIO

### Understanding Error Models

```scala
// ZIO[R, E, A]
// R - Environment (requirements)
// E - Error type (can be Nothing for infallible effects)
// A - Success type

// Infallible effect (never fails)
val infallible: ZIO[Any, Nothing, String] = ZIO.succeed("Hello")

// Failable effect (can fail with Throwable)
val failable: ZIO[Any, Throwable, String] = ZIO.attempt("Hello".charAt(5))

// Typed error
val typedError: ZIO[Any, String, Int] = ZIO.fail("Invalid input")
```

## 2. Basic Error Handling

### Creating Effects That Can Fail

```scala
// From total functions
val effect = ZIO.succeed(42)

// From partial functions (can throw)
val safeParse = ZIO.attempt(parseInt("42"))

// From Either
val fromEither = ZIO.fromEither(Right(42))

// From Option
val fromOption = ZIO.fromOption(Some(42))

// From Try
val fromTry = ZIO.fromTry(Try(42))
```

### Handling Errors

```scala
// Recover with default value
val recovered = effect.catchAll(_ => ZIO.succeed(0))

// Recover for specific error
val recoveredSpecific = effect.catchSome {
  case _: NumberFormatException => ZIO.succeed(0)
}

// Fallback to alternative effect
val fallback = effect.orElse(ZIO.succeed(0))

// Retry on failure
val retried = effect.retry(Schedule.exponentialBackoff(100.millis))
```

## 3. Error Accumulation

### Validated for Accumulating Errors

```scala
import zio.prelude._

// Use Validation instead of Either for accumulation
case class Config(
  host: String,
  port: Int,
  username: String
)

object Config {
  def validate(host: String, port: String, username: String): Validation[String, Config] = {
    val validHost = Validation.succeed(host)
    val validPort = Validation.fromEither(port.toIntOption.toRight("Invalid port"))
    val validUser = Validation.succeed(username)
    
    (validHost, validPort, validUser).mapN(Config(_, _, _))
  }
}
```

### ZIO.foreach vs ZIO.foreachPar for Validation

```scala
// Sequential validation (fails fast)
val validateSequential = ZIO.foreach(items)(validate)

// Parallel validation (accumulates errors)
val validateParallel = ZIO.validatePar(items)(validate)
```

## 4. Error Refinement

### Refine Errors to Specific Types

```scala
// Generic error
def parse(input: String): ZIO[Any, Throwable, Int] =
  ZIO.attempt(input.toInt)

// Refine to specific error
def parseRefined(input: String): ZIO[Any, NumberFormatException, Int] =
  ZIO.attempt(input.toInt).mapError {
    case nfe: NumberFormatException => nfe
    case other => new NumberFormatException(other.getMessage)
  }

// Or use refineOrDie
def parseRefinedOrDie(input: String): ZIO[Any, NumberFormatException, Int] =
  ZIO.attempt(input.toInt).refineToOrDie[NumberFormatException]
```

### Error Adapters

```scala
// Transform error type
val adapted: ZIO[Any, String, Int] = 
  effect.mapError(_.getMessage)

// Combine with context
val contextual: ZIO[Any, String, Int] =
  effect.mapError(err => s"Failed to parse: ${err.getMessage}")
```

## 5. Resource Cleanup

### Ensuring Cleanup on Errors

```scala
// acquireRelease ensures cleanup even on failure
val resource = ZIO.acquireRelease(
  acquire = openFile()
)(
  release = file => file.close().orDie
)

// Using scope
val scoped = ZIO.scoped {
  for {
    file <- ZIO.fromAutoCloseable(java.io.FileReader("file.txt"))
    content <- ZIO.attempt(file.read())
  } yield content
}
```

### Error Handling in Resources

```scala
// Ensure cleanup happens even when processing fails
val processFile = ZIO.acquireRelease(
  acquire = ZIO.attempt(openConnection())
)(
  release = conn => ZIO.attempt(conn.close()).orDie
).flatMap { conn =>
  for {
    data <- ZIO.attempt(conn.read())
    _    <- processData(data) // May fail
  } yield result
}
```

## 6. Logging Errors

### Structured Error Logging

```scala
val logged = effect.catchAll { error =>
  ZIO.logError(s"Operation failed: ${error.getMessage}") *>
  ZIO.succeed(defaultValue)
}

// With cause
val loggedWithCause = effect.tapError { error =>
  ZIO.logErrorCause("Operation failed", Cause.fail(error))
}
```

### Error Context

```scala
// Add context to errors
val withContext = effect.mapError { error =>
  new RuntimeException(s"Failed in context: $context", error)
}

// Or use ZIO.unit for context
val contextual = ZIO.unit.flatMap { _ =>
  effect
}.mapError { error =>
  new RuntimeException(s"Failed in context", error)
}
```

## 7. Error Recovery Patterns

### Circuit Breaker Pattern

```scala
def circuitBreaker[R, E, A](
  effect: ZIO[R, E, A],
  maxFailures: Int = 5,
  resetTimeout: Duration = 1.minute
): ZIO[R, E, A] = {
  // Implementation would track failures and open circuit
  effect
}
```

### Fallback Patterns

```scala
// Primary with fallback
val withFallback = primaryEffect.orElse(backupEffect)

// Multiple fallbacks
val multipleFallbacks = 
  primaryEffect.orElse(secondaryEffect).orElse(tertiaryEffect)

// Graceful degradation
val degrade = primaryEffect.catchAll { _ =>
  ZIO.succeed(degradedValue)
}
```

### Retry with Backoff

```scala
// Exponential backoff
val retried = effect.retry(
  Schedule.exponentialBackoff(100.millis, 2.0) && Schedule.recurs(3)
)

// Fixed delay
val retriedFixed = effect.retry(Schedule.fixed(1.second))

// Spaced out
val spaced = effect.retry(Schedule.spaced(500.millis))
```

## 8. Error Testing

### Testing Error Cases

```scala
import zio.test._
import zio.test.Assertion._

object ErrorHandlingSpec extends ZIOBaseSpec {
  def spec = suite("Error Handling")(
    test("handles invalid input") {
      for {
        result <- parseInt("invalid").either
      } yield assert(result)(isLeft(isSubtype[String](anything)))
    },
    
    test("recovers from error") {
      for {
        result <- parseInt("invalid").catchAll(_ => ZIO.succeed(0))
      } yield assert(result)(equalTo(0))
    }
  )
}
```

### Property-Based Testing for Errors

```scala
test("never fails for valid inputs") {
  check(Gen.int) { input =>
    parseInt(input.toString).mapError(_.getMessage).either.map {
      case Right(value) => assertTrue(value == input)
      case Left(_)      => assertTrue(false) // Should not fail
    }
  }
}
```

## 9. Common Pitfalls

### ❌ Don't

```scala
// Swallow errors silently
effect.catchAll(_ => ZIO.unit)

// Use try-catch in ZIO
ZIO.succeed(try parse() catch { case e => throw e })

// Ignore error type
val any: ZIO[Any, Any, String] = effect.asInstanceOf[ZIO[Any, Any, String]]

// Blocking operations in error handling
effect.catchAll { _ =>
  Thread.sleep(1000) // Don't block!
  ZIO.succeed(0)
}
```

### ✅ Do

```scala
// Handle errors appropriately
effect.catchAll { error =>
  ZIO.logError(s"Failed: $error") *>
  ZIO.succeed(default)
}

// Use ZIO.attempt for partial functions
ZIO.attempt(parse())

// Preserve error types
effect.mapError[SpecificError](err => SpecificError(err.getMessage))

// Asynchronous error handling
effect.catchAll { _ =>
  ZIO.sleep(1.second) *> ZIO.succeed(0)
}
```

## 10. Advanced Patterns

### Error Channels

```scala
// Separate error channels
sealed trait AppError
case class ValidationError(msg: String) extends AppError
case class DatabaseError(msg: String) extends AppError

def validate(input: String): ZIO[Any, ValidationError, Config] = ???
def query(config: Config): ZIO[Any, DatabaseError, Data] = ???

// Compose with typed errors
val program = for {
  config <- validate
  data   <- query(config)
} yield data
```

### Error Telemetry

```scala
// Track error rates
val withTelemetry = effect.tapError { error =>
  ZIO.service[Metrics].flatMap { metrics =>
    metrics.incrementCounter("errors", Map("error_type" -> error.getClass.getSimpleName))
  }
}
```

## Summary

**Key Principles:**

1. **Use typed errors** when possible
2. **Handle errors appropriately** - don't swallow them
3. **Clean up resources** even on failure
4. **Log errors with context**
5. **Test error cases**
6. **Use ZIO's error handling** instead of try-catch
7. **Consider error accumulation** for validation

**Common Patterns:**

- `ZIO.attempt` for partial functions
- `.catchAll` for recovery
- `.orElse` for fallbacks
- `.retry` for transient failures
- `.refineToOrDie` for error refinement
- `ZIO.acquireRelease` for cleanup

## Further Reading

- [ZIO Error Management](https://zio.dev/guide/appendix/errors)
- [Error Accumulation](https://zio.dev/guide/zioapp/observability)
- [Testing Effects](https://zio.dev/guide/test/testing)
