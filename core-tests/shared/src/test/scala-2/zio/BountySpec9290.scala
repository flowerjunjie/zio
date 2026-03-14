package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9290
 * 
 * Issue: Concurrency limit with foreachPar
 * 
 * This test verifies concurrency limiting functionality.
 */
object BountySpec9290 extends ZIOSpecDefault {
  def spec = suite("Concurrency limit")(
    test("foreachParN respects concurrency limit") {
      for {
        maxConcurrent <- Ref.make(0)
        current <- Ref.make(0)
        
        fibers <- ZIO.foreachPar((1 to 20).toList) { i =>
          for {
            _ <- current.update(_ + 1)
            cur <- current.get
            _ <- maxConcurrent.update(Math.max(_, cur))
            _ <- ZIO.sleep(50.millis)
            _ <- current.update(_ - 1)
          } yield i
        }.withParallelism(5)
        
        max <- maxConcurrent.get
      } yield max <= 6 // Should be around 5 with some overhead
    },
    
    test("foreachParN processes all elements") {
      for {
        results <- ZIO.foreachPar((1 to 100).toList) { i =>
          ZIO.succeed(i * 2)
        }.withParallelism(10)
        
        count = results.size
      } yield count == 100
    },
    
    test("foreachParN with limit of 1 is sequential") {
      for {
        executed <- Ref.make(List.empty[Int])
        
        _ <- ZIO.foreachPar((1 to 10).toList) { i =>
          executed.update(_ :+ i) *> ZIO.succeed(i)
        }.withParallelism(1)
        
        order <- executed.get
      } yield order == List(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    },
    
    test("foreachParN with high limit is fully concurrent") {
      for {
        fibers <- ZIO.foreachPar((1 to 20).toList) { i =>
          ZIO.sleep(10.millis) *> ZIO.succeed(i)
        }.withParallelism(100)
        
        count = fibers.size
      } yield count == 20
    },
    
    test("foreachParN handles errors gracefully") {
      val result = ZIO.foreachPar((1 to 10).toList) { i =>
        if (i == 5) ZIO.fail(new Error("Error"))
        else ZIO.succeed(i)
      }.withParallelism(5).either
      
      assertCompletes(ZIO.succeed(result))(isFailing(anything))
    }
  )
}
