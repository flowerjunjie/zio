# Easier Derivation of Config with Scala 3

## Overview
This document describes improvements for Config derivation in Scala 3, making it easier and more intuitive to derive configurations from case classes.

## Current Issues
- Config derivation in Scala 3 requires manual boilerplate
- Type inference can be unclear
- Error messages are not always helpful
- Macro system differences between Scala 2 and 3

## Proposed Improvements

### 1. Simplified Derivation API
```scala
import zio.Config._

// Before (verbose)
case class User(name: String, age: Int)
val userConfig: Config[User] = (string("name") ++ int("age")).to[User]

// After (simple)
case class User(name: String, age: Int)
val userConfig: Config[User] = Config.derived[User]
```

### 2. Automatic Field Naming
```scala
case class DatabaseConfig(
  host: String,
  port: Int,
  username: String,
  password: String
)

// Fields automatically mapped to config keys
val dbConfig = Config.derived[DatabaseConfig]
```

### 3. Nested Config Support
```scala
case class ServerConfig(
  database: DatabaseConfig,
  cache: CacheConfig
)

// Automatically derives nested configurations
val serverConfig = Config.derived[ServerConfig]
```

## Scala 3 Macro Implementation
```scala
import scala.quoted._

inline def derived[A]: Config[A] = ${derivedImpl[A]}

def derivedImpl[A: Type](using Quotes): Expr[Config[A]] = {
  import quotes.reflect._
  
  val tpe = TypeRepr.of[A]
  
  // Generate Config from case class fields
  // Implementation details...
}
```

## Related
- Issue: #9268
