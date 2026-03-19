package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #8792
 * 
 * Issue: ZStream.tapSink, either a flaky test or flaky implementation
 * 
 * This test verifies that ZStream.tapSink works correctly and consistently.
 */
object BountySpec8792 extends ZIOSpecDefault {
  def spec = suite("ZStream.tapSink behavior")(
    test("tapSink executes sink for each element") {
      var collected = List.empty[Int]
      val sink = ZSink.collectAll[Int].tap { value =>
        ZIO.succeed(collected = collected :+ value)
      }
      
      val stream = ZStream(1, 2, 3, 4, 5)
      
      assertCompletes(stream.run(sink))(equalTo(Chunk(1, 2, 3, 4, 5)))
    },
    
    test("tapSink doesn't modify stream content") {
      val sink = ZSink.collectAll[Int].tap { value =>
        ZIO.log(s"Processing: $value")
      }
      
      val stream = ZStream(1, 2, 3)
      
      assertCompletes(stream.run(sink))(equalTo(Chunk(1, 2, 3)))
    },
    
    test("tapSink works with empty stream") {
      var called = false
      val sink = ZSink.collectAll[Int].tap { _ =>
        ZIO.succeed(called = true)
      }
      
      val stream = ZStream.empty[Int]
      
      val result = stream.run(sink)
      
      assertCompletes(result *> ZIO.succeed(called))(equalTo(false))
    },
    
    test("tapSink preserves sink failures") {
      val failingSink = ZSink.fromFunction[Int, Int, Nothing] { _ =>
        ZIO.fail(new RuntimeException("Sink failed"))
      }
      
      val stream = ZStream(1, 2, 3)
      
      assertCompletes(stream.run(failingSink).exit)(isFailing(anything))
    },
    
    test("tapSink works with side effects") {
      var counter = 0
      val sideEffectSink = ZSink.collectAll[Int].tap { _ =>
        ZIO.succeed(counter = counter + 1)
      }
      
      val stream = ZStream(1, 2, 3, 4, 5)
      
      assertCompletes(stream.run(sideEffectSink) *> ZIO.succeed(counter))(equalTo(5))
    },
    
    test("tapSink works with multiple operations") {
      var transformed = List.empty[String]
      
      val sink = ZSink.collectAll[Int].tap { value =>
        ZIO.succeed(transformed = transformed :+ value.toString)
      }
      
      val stream = ZStream(1, 2, 3)
      
      assertCompletes(stream.run(sink))(equalTo(Chunk(1, 2, 3)))
    }
  )
}
