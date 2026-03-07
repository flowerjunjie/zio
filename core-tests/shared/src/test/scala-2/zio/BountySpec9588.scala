package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9588
 * 
 * Issue: Error propagation in concurrent code
 * 
 * This test verifies error propagation behavior.
 */
object BountySpec9588 extends ZIOSpecDefault {
  def spec = suite("Error propagation in concurrent code")(
    test("errors propagate through foreachPar") {
      val result = ZIO.foreachPar((1 to 5).toList) { i =>
        if (i == 3) ZIO.fail(new Error(s"Error $i"))
        else ZIO.succeed(i)
      }.either
      
      assertCompletes(ZIO.succeed(result))(isFailing(anything))
    },
    
    test("errors propagate through race") {
      val result = ZIO.fail(new Error("Failed"))
        .race(ZIO.sleep(100.millis) *> ZIO.succeed(1))
        .either
      
      assertCompletes(ZIO.succeed(result))(isFailing(anything))
    },
    
    test("errors in finalizers are not lost") {
      for {
        logged <- Ref.make(List.empty[String])
        
        result <- ZIO.acquireReleaseExit(
          ZIO.unit
        ) { _ =>
          ZIO.fail(new Error("Failed")) *> 
          logged.update(_ :+ "cleanup")
        }.either
        
        log <- logged.get
      } yield result.isLeft && log.contains("cleanup")
    },
    
    test("errors are preserved through async boundaries") {
      for {
        result <- ZIO.async[Any, Error, Int] { k =>
          ZIO.runtime[Any].flatMap { rt =>
            rt.unsafe.run(
              ZIO.succeed(42).flatMap(k).unit
            )
          }
        }.either
        
        r <- ZIO.succeed(result)
      } yield r.isRight
    },
    
    test("errors can be recovered in concurrent code") {
      for {
        recovered <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 5).toList) { i =>
          ZIO.fail(new Error(s"Error $i"))
            .catchAll(_ => recovered.update(_ + 1))
        }
        
        count <- recovered.get
      } yield count == 5
    },
    
    test("error messages are informative") {
      val error = new Error("Detailed error message")
      
      result <- ZIO.fail(error).either
      
      msg <- result.swap.map(_.getMessage)
      
      assertCompletes(ZIO.succeed(msg))(equalTo("Detailed error message"))
    }
  )
}
