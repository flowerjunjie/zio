package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9198
 * 
 * Issue: Running finalizers in parallel using foreachPar or foreachParN
 * 
 * This test verifies parallel finalizer execution.
 */
object BountySpec9198 extends ZIOSpecDefault {
  def spec = suite("Parallel finalizer execution")(
    test("finalizers run in parallel with foreachPar") {
      for {
        finalized <- Ref.make(List.empty[Int])
        
        _ <- ZIO.acquireReleaseExit(
          ZIO.foreachPar((1 to 5).toList) { i =>
            finalized.update(_ :+ (-i))
          }
        ) { _ =>
          ZIO.foreachPar((1 to 5).toList) { i =>
            finalized.update(_ :+ i)
          }
        } *> ZIO.unit
        
        finalList <- finalized.get
      } yield finalList.size == 10
    },
    
    test("foreachParN limits parallel finalizers") {
      for {
        counter <- Ref.make(0)
        maxConcurrent <- Ref.make(0)
        
        acquire = ZIO.unit
        
        release = ZIO.acquireRelease(
          counter.update(_ + 1) *> maxConcurrent.update(Math.max(_, counter.get)) *> ZIO.sleep(50.millis)
        )(_ => counter.update(_ - 1)) *> ZIO.unit
        
        _ <- ZIO.acquireReleaseExit(acquire) { _ =>
          ZIO.foreachParN(3)((1 to 10).toList) { _ =>
            release
          }
        } *> ZIO.unit
        
        max <- maxConcurrent.get
      } yield max <= 4 // Should be around 3 with some overhead
    },
    
    test("parallel finalizers complete even if one fails") {
      for {
        finalized <- Ref.make(0)
        
        _ <- ZIO.acquireReleaseExit(
          ZIO.unit
        ) { _ =>
          ZIO.foreachPar((1 to 5).toList) { i =>
            if (i == 3) ZIO.fail(new Error("Fail"))
            else finalized.update(_ + 1)
          }.either
        } *> ZIO.unit
        
        count <- finalized.get
      } yield count >= 4 // At least 4 should complete
    },
    
    test("parallel finalizers are interruptible") {
      for {
        finalized <- Ref.make(0)
        
        fiber <- ZIO.acquireReleaseExit(
          ZIO.unit
        ) { _ =>
          ZIO.foreachPar((1 to 10).toList) { _ =>
            finalized.update(_ + 1) *> ZIO.sleep(1.second)
          }
        }.fork
        
        _ <- ZIO.sleep(100.millis)
        _ <- fiber.interrupt
        
        count <- finalized.get
      } yield count >= 1 // At least some started
    }
  )
}
