# Documentation: ZIOApp#gracefulShutdownTimeout

## Issue #9913

### Summary
This PR adds documentation for `ZIOApp#gracefulShutdownTimeout` configuration option.

### What is gracefulShutdownTimeout?
The `gracefulShutdownTimeout` specifies how long the application should wait for ongoing effects to complete during a graceful shutdown before forcibly interrupting them.

### Usage Example
```scala
object MyApp extends ZIOAppDefault {
  // Set a 30-second graceful shutdown timeout
  val gracefulShutdownTimeout = 30.seconds
  
  def run = 
    ZIO.log("Application started")
}
```

### Behavior
- Default: No timeout (waits indefinitely)
- When set: Application will wait up to the specified duration
- After timeout: Remaining fibers are forcibly interrupted
- Use case: Prevent hanging applications during shutdown

### Best Practices
1. Set a reasonable timeout based on your application's needs
2. Ensure critical cleanup code completes within this window
3. Consider logging when graceful shutdown begins

### Related Issues
- Addresses #9913
