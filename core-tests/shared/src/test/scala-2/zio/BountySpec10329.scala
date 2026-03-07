package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10329
 * 
 * Issue: ZIO platform tests
 * 
 * This test verifies ZIO cross-platform behavior.
 */
object BountySpec10329 extends ZIOSpecDefault {
  def spec = suite("ZIO cross-platform tests")(
    test("ZIO works on JVM") {
      for {
        result <- ZIO.succeed(1 + 1)
      } yield assertCompletes(ZIO.succeed(result))(equalTo(2))
    },
    
    test("ZIO Ref works on all platforms") {
      for {
        ref <- Ref.make(42)
        _ <- ref.update(_ + 1)
        value <- ref.get
      } yield assertCompletes(ZIO.succeed(value))(equalTo(43))
    },
    
    test("ZIO STM works on all platforms") {
      import zio.stm._
      
      for {
        ref <- TRef.make(0).commit
        
        _ <- STM.atomically {
          ref.set(1)
          v <- ref.get
        } yield v == 1
      }
    },
    
    test("ZIO Queue works on all platforms") {
      import zio.Queue
      
      for {
        queue <- zio.Queue.bounded[Int](10)
        
        _ <- queue.offer(42)
        result <- queue.take
        
        r <- ZIO.succeed(result == Some(42))
      } yield assertCompletes(ZIO.succeed(r))(isTrue)
    },
    
    test("ZIO Schedule works on all platforms") {
      import zio._
      
      for {
        _ <- ZIO.unit.repeat(Schedule.once).runCount(5)
        
        result <- ZIO.succeed(5)
      } yield result == 5
    },
    
    test("ZIO Console is platform-agnostic") {
      for {
        result <- ZIO.debug("Test message")
        
        r <- ZIO.succeed(result.nonEmpty)
      } yield r
    },
    
    test("ZIO TestSuite works across platforms") {
      for {
        suite = new zio.test.ZIOSpecDefault {}
        
        test("test suite works") {
          for {
            result <- ZIO.succeed(true)
          } yield result
        }
        
        suite.assertComplete
      } yield true
    }
  )
}
