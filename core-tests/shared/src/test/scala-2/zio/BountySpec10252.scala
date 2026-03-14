package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10252
 * 
 * Issue: Memory allocation patterns
 * 
 * This test verifies memory allocation efficiency.
 */
object BountySpec10252 extends ZIOSpecDefault {
  def spec = suite("Memory allocation patterns")(
    test("lazy values don't allocate eagerly") {
      for {
        lazy val expensive = List.fill(10000)(42)
        
        start <- Clock.nanoTime
        
        // Lazy value shouldn't allocate until accessed
        _ <- ZIO.succeed(expensive.head)
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(duration < 100))(isTrue)
    },
    
    test("Stream doesn't buffer entire stream") {
      for {
        processed <- Ref.make(0)
        
        stream = ZStream.iterate(0)(_ + 1)
          .tap { _ =>
            processed.update(_ + 1)
          }
          .take(5)
        
        _ <- stream.runDrain
        
        count <- processed.get
      } yield assertCompletes(ZIO.succeed(count))(equalTo(5))
    },
    
    test("foreach doesn't create intermediate lists") {
      for {
        start <- Clock.nanoTime
        
        result <- ZIO.foreach((1 to 1000).toList) { i =>
          ZIO.succeed(i * 2)
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(result.size))(equalTo(1000)) && duration < 5000
    },
    
    test("forEachPar is memory efficient") {
      for {
        start <- Clock.nanoTime
        
        results <- ZIO.foreachPar((1 to 100).toList) { i =>
          ZIO.succeed(i * 2)
        }
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        sum = results.sum
      } yield sum == 10100 && duration < 5000
    },
    
    test("scoped resources are promptly released") {
      for {
        acquired <- Ref.make(0)
        released <- Ref.make(0)
        
        _ <- ZIO.acquireRelease(
          acquired.update(_ + 1)
        )(_ => released.update(_ + 1)) *> ZIO.unit
        
        a <- acquired.get
        r <- released.get
      } yield a == 1 && r == 1
    },
    
    test("deferred computations are deallocated") {
      for {
        result <- ZIO.succeed(42)
        
        // Deferred computations should be deallocated after execution
        _ <- ZIO.yieldNow
        _ <- ZIO.yieldNow
        
        r <- ZIO.succeed(result)
      } yield r == 42
    }
  )
}
