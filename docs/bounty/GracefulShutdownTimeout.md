# ZIOApp Graceful Shutdown Timeout

## Overview
The `gracefulShutdownTimeout` configuration option allows you to specify a timeout for the graceful shutdown process of your ZIO application.

## What is Graceful Shutdown?
Graceful shutdown is the process of:
1. Stopping accepting new work
2. Waiting for in-progress work to complete
3. Cleaning up resources
4. Then terminating

## Configuration
```scala
object MyApp extends ZIOAppDefault {
  // Set a 30-second timeout for graceful shutdown
  val gracefulShutdownTimeout = 30.seconds
  
  def run = 
    ZIO.log("Application started")
}
```

## Behavior
| Scenario | Behavior |
|----------|----------|
| No timeout set | Wait indefinitely for all fibers to complete |
| Timeout reached | Forcibly interrupt remaining fibers |
| Timeout not reached | Complete shutdown naturally |

## Use Cases
1. **Production Deployments**: Prevent hanging deployments
2. **Long-running Tasks**: Set appropriate timeout for cleanup
3. **Resource Cleanup**: Ensure database connections close properly

## Best Practices
- Set timeout based on your application's cleanup needs
- Log when graceful shutdown starts
- Test shutdown behavior in staging
- Consider timeouts for different environments (dev vs prod)

## Example
```scala
object MyApp extends ZIOAppDefault {
  val gracefulShutdownTimeout = 30.seconds
  
  def run = 
    for {
      _ <- ZIO.log("Starting application")
      _ <- ZIO.sleep(1.minute)
      _ <- ZIO.log("Application running")
    } yield ExitCode.success
}
```

## Related
- Issue: #9913
- ZIOApp Documentation: https://zio.dev/zio-app
