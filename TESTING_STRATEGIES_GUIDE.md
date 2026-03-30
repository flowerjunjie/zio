# ZIO Testing Strategies - Complete Guide

## Overview

Testing in ZIO is powerful and expressive. This guide covers testing strategies from unit tests to property-based testing.

## 1. Test Setup

### Basic Test Structure

```scala
import zio.test._
import zio.test.Assertion._

object MySpec extends ZIOBaseSpec {
  def spec = suite("My Suite")(
    test("addition works") {
      assertTrue(1 + 1 == 2)
    }
  )
}
```

### Using ZIO Test Effects

```scala
test("async operation") {
  for {
    result <- ZIO.succeed(42)
  } yield assertTrue(result == 42)
}
```

## 2. Assertion Reference

### Basic Assertions

```scala
// Equality
assertTrue(a == b)
assert(a)(equalTo(b))

// Inequality
assertTrue(a != b)
assert(a)(not(equalTo(b)))

// Boolean
assertTrue(condition)
assertFalse(condition)

// Null checks
assert(actual)(isNull)
assert(actual)(isNotNull)

// Instance checks
assert(value)(isSubtype[String](anything))

// Throwing
assert(effect)(throws[NumberFormatException])
```

### Numeric Assertions

```scala
// Comparisons
assert(n)(isGreaterThan(0))
assert(n)(isGreaterThanOrEqualTo(0))
assert(n)(isLessThan(100))
assert(n)(isLessThanOrEqualTo(100))

// Ranges
assert(n)(isBetween(0, 100))

// Close to (for floating point)
assert(3.14)(isCloseTo(3.14159, 0.01))
```

### Collection Assertions

```scala
// Size
assert(list)(hasSize(5))

// Contains
assert(list)(contains(element))

// ForAll
assert(list)(forall(isGreaterThan(0)))

// Exists
assert(list)(exists(isGreaterThan(0)))

// Empty
assert(list)(isEmpty)
assert(list)(isNotEmpty)

// Same elements
assert(list1)(sameElementsAs(list2))
```

### String Assertions

```scala
// Substring
assert(str)(containsSubstring("hello"))

// Starts with
assert(str)(startsWith("hello"))

// Ends with
assert(str)(endsWith("world"))

// Matches regex
assert(str)(matchesRegex("^[A-Z].*"))
```

### Option/Either Assertions

```scala
// Option
assert(opt)(isSome(isGreaterThan(0)))
assert(opt)(isNone)

// Either
assert(either)(isRight(isGreaterThan(0)))
assert(either)(isLeft(isSubtype[String](anything)))
```

## 3. Test Suites

### Organizing Tests

```scala
object MySpec extends ZIOBaseSpec {
  def spec = suite("My Suite")(
    suite("Feature 1")(
      test("scenario 1")(testLogic1),
      test("scenario 2")(testLogic2)
    ),
    suite("Feature 2")(
      test("scenario 3")(testLogic3)
    )
  )
}
```

### Shared Setup

```scala
object MySpec extends ZIOSpecDefault {
  // Shared layer available to all tests
  val myLayer = ZLayer.succeed(MyService.live)

  override def spec = suite("My Suite")(
    test("uses shared service") {
      for {
        service <- ZIO.service[MyService]
        result   <- service.doWork()
      } yield assertTrue(result == "expected")
    }
  ).provideLayer(myLayer)
}
```

## 4. Property-Based Testing

### Basic Property Tests

```scala
import zio.test.Gen

test("list reverse is involutive") {
  check(Gen.listOf(Gen.int)) { list =>
    assert(list.reverse.reverse)(equalTo(list))
  }
}
```

### Custom Generators

```scala
// Simple generator
val positiveInt: Gen[Any, Int] = Gen.int.map(_.abs)

// Filtered generator
val evenNumber: Gen[Any, Int] = Gen.int.filter(_ % 2 == 0)

// Frequency
val mixed: Gen[Any, String] = Gen.frequency(
  10 -> Gen.string,     // 10% string
  30 -> Gen.alphaNumeric, // 30% alphanumeric
  60 -> Gen.const("fixed") // 60% fixed value
)

// Option generator
val optionalInt: Gen[Any, Option[Int]] = Gen.option(Gen.int)

// Either generator
val either: Gen[Any, Either[String, Int]] = Gen.either(
  Gen.string,
  Gen.int
)
```

### Advanced Properties

```scala
test("map preserves size") {
  check(Gen.mapOf(Gen.string, Gen.int)) { map =>
    assert(map.map(_ * 2).size)(equalTo(map.size))
  }
}

test("string split and join are inverse") {
  check(Gen.string, Gen.ascii.string) { (str, delimiter) =>
    assert(str.split(delimiter).mkString(delimiter))(equalTo(str))
  }
}
```

## 5. Testing Effects

### Testing Success Cases

```scala
test("effect succeeds") {
  for {
    result <- ZIO.succeed(42)
  } yield assertTrue(result == 42)
}
```

### Testing Failure Cases

```scala
test("effect fails with specific error") {
  val effect = ZIO.fail("error")
  
  for {
    result <- effect.either
  } yield assert(result)(isLeft(equalTo("error")))
}
```

### Testing Timeout

```scala
test("effect completes within timeout") {
  val effect = ZIO.sleep(100.millis) *> ZIO.succeed(42)
  
  assertZIO(effect)(equalTo(42)).provideSome[Clock](Clock.ClockLive)
    .timeout(1.second)
}
```

## 6. Mocking and Test Doubles

### Using Test Services

```scala
// Define test service
object TestUserService {
  def getUser(id: String): ZIO[Any, Nothing, Option[User]] =
    ZIO.succeed(Some(User("test", id)))
}

// Use in tests
test("greets user") {
  for {
    greeting <- greetUser("123").provideSome[UserService](TestUserService)
  } yield assertTrue(greeting == "Hello test")
}
```

### Layered Testing

```scala
val testLayer = ZLayer.succeed(
  new UserService {
    override def getUser(id: String): ZIO[Any, Nothing, Option[User]] =
      ZIO.succeed(Some(User("test", id)))
  }
)

test("uses test service") {
  val program = for {
    user <- ZIO.serviceWithZIO[UserService](_.getUser("123"))
  } yield user

  program.assertSome(
    assertThat(_.name)(equalTo("test"))
  ).provideLayer(testLayer)
}
```

## 7. Test Aspects

### Reusable Test Modifiers

```scala
import zio.test.TestAspect

// Ignore test
test("not yet implemented") {
  assertTrue(true)
} @@ TestAspect.ignore

// Only run on JVM
test("JVM specific") {
  assertTrue(true)
} @@ TestAspect.jvmOnly

// Timeout test
test("completes quickly") {
  ZIO.sleep(100.millis)
} @@ TestAspect.timeout(1.second)

// Retry flaky tests
test("sometimes flaky") {
  // May fail occasionally
  assertTrue(Random.nextBoolean())
} @@ TestAspect.flaky
```

### Custom Aspects

```scala
val onlyInProduction = new TestAspect {
  def apply[R](spec: Spec[R]) = 
    if (sys.env.get("ENV") == Some("prod")) spec
    else Spec.ignore("Production only")
}
```

## 8. Integration Testing

### Testing with Real Services

```scala
test("database integration") {
  val layer = ZLayer.scoped {
    for {
      conn <- ZIO.attempt(createTestConnection())
      _    <- ZIO.addFinalizer(conn.close())
    } yield new DatabaseService {
      def query(sql: String) = ZIO.attempt(conn.executeQuery(sql))
    }
  }

  val program = for {
    db      <- ZIO.service[DatabaseService]
    result  <- db.query("SELECT 1")
  } yield result

  program.provideLayer(layer)
}
```

### Test Containers

```scala
import zio.test.TestAspect

test("with test container") {
  // Use Docker containers for integration tests
  ZIO.serviceWithZIO[Container] { container =>
    // Run tests against real database in container
  }
} @@ TestAspect.integration
```

## 9. Performance Testing

### Benchmarking

```scala
test("processes 1000 items in < 1s") {
  for {
    start   <- Clock.nanoTime
    _       <- processItems(1000)
    end     <- Clock.nanoTime
    duration = (end - start) / 1000000 // to ms
  } yield assertTrue(duration < 1000)
}
```

### Load Testing

```scala
test("handles 1000 concurrent requests") {
  val loadTest = ZIO.foreachPar(1000)(i => apiCall(i))
  
  for {
    results <- loadTest
  } yield assert(results)(hasSize(1000))
}
```

## 10. Testing Concurrent Code

### Testing Race Conditions

```scala
test("no race condition in counter") {
  for {
    counter <- Ref.make(0)
    _       <- ZIO.foreachPar(1000)(_ => counter.update(_ + 1))
    result  <- counter.get
  } yield assertTrue(result == 1000)
}
```

### Testing Fiber Interruption

```scala
test("cleanup on interruption") {
  for {
    ref     <- Ref.make(false)
    promise <- Promise.make[Nothing, Unit]
    effect   = (promise.succeed(()) *> ZIO.never)
                .ensuring(ref.set(true))
    fiber   <- effect.fork
    _       <- promise.await
    _       <- fiber.interrupt
    cleaned <- ref.get
  } yield assertTrue(cleaned)
}
```

## 11. Test Organization

### Package Structure

```
src/test/scala/
├── zio/
│   ├── service/
│   │   ├── UserServiceSpec.scala
│   │   └── DatabaseServiceSpec.scala
│   ├── integration/
│   │   └── APISpec.scala
│   └── performance/
│       └── LoadTestSpec.scala
```

### Shared Test Utilities

```scala
package test

object TestHelpers {
  def await[A](f: Fiber[Nothing, A]): ZIO[Any, Nothing, A] =
    f.join

  def eventually[R, E](effect: ZIO[R, E, Any], timeout: Duration = 5.seconds): ZIO[R, E, Any] =
    effect.retry(Schedule.spaced(100.millis) && Schedule.elapsed(timeout))
}
```

## 12. Common Patterns

### Given-When-Then

```scala
test("user can log in") {
  // Given
  val username = "test"
  val password = "pass"
  
  // When
  val result = login(username, password)
  
  // Then
  assertZIO(result)(isSome)
}
```

### Setup-Exercise-Verify

```scala
test("service processes items") {
  for {
    // Setup
    service <- ZIO.service[ProcessingService]
    input   = List(1, 2, 3)
    
    // Exercise
    result <- service.processBatch(input)
    
    // Verify
  } yield assert(result)(hasSize(3))
}
```

## 13. Debugging Tests

### Printing Test Values

```scala
test("debug output") {
  for {
    value <- compute()
    _     <- ZIO.debug(s"Value: $value")
  } yield assertTrue(true)
}
```

### Tracing Execution

```scala
test("trace execution") {
  ZIO.succeed(42)
    .traceExecution
    .map(result => assertTrue(result == 42))
}
```

## Summary

**Key Principles:**

1. **Test behavior, not implementation**
2. **Use property-based testing** for algorithms
3. **Test edge cases and error cases**
4. **Keep tests simple and focused**
5. **Use test aspects** for common modifiers
6. **Mock external dependencies**
7. **Test concurrent code carefully**

**Common Patterns:**

- `assertTrue` for simple assertions
- `check` for property-based tests
- `assertZIO` for effectful assertions
- `@@ TestAspect` for test modifiers
- `.provideLayer` for dependency injection

**Further Reading:**

- [ZIO Testing Guide](https://zio.dev/guide/test)
- [Assertion Reference](https://zio.dev/reference/test/assertions)
- [Test Aspects](https://zio.dev/reference/test/aspects)
