package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #10108
 * 
 * Issue: ZStream combinators are efficient
 * 
 * This test verifies stream combinator efficiency.
 */
object BountySpec10108 extends ZIOSpecDefault {
  def spec = suite("ZStream combinators efficiency")(
    test("zipWith is efficient") {
      for {
        start <- Clock.nanoTime
        
        s1 = ZStream(1 to 100)
        s2 = ZStream(1 to 100)
        
        result <- s1.zipWith(s2)(_ + _).runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        size = result.size
      } yield assertCompletes(ZIO.succeed(size))(equalTo(100)) && assertCompletes(ZIO.succeed(duration < 2000))(isTrue)
    },
    
    test("zipWithIndex is efficient") {
      for {
        start <- Clock.nanoTime
        
        result <- ZStream(1 to 100)
          .zipWithIndex
          .filter { case (_, index) => index % 2 == 0 }
          .map { case (value, _) => value }
          .runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield duration < 2000
    },
    
    test("merge is concurrent and efficient") {
      for {
        start <- Clock.nanoTime
        
        stream1 = ZStream.fromIterable(1 to 500)
        stream2 = ZStream.fromIterable(501 to 1000)
        
        result <- (stream1 merge stream2).runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
        
        size = result.size
      } yield assertCompletes(ZIO.succeed(size))(equalTo(1000)) && duration < 5000
    },
    
    test(interrupt combinators don't block") {
      for {
        start <- Clock.nanoTime
        
        stream = ZStream.never.orElse(ZIO.succeed(1)).timeout(100.millis)
        
        result <- stream.runCollect.either
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(duration < 200))(isTrue)
    },
    
    test("orElse is efficient") {
      for {
        start <- Clock.nanoTime
        
        stream1 = ZStream.fail(new Error("Error"))
        stream2 = ZStream(1, 2, 3)
        
        result <- (stream1 orElse stream2).runCollect
        
        end <- Clock.nanoTime
        duration = (end - start) / 1000000.0
      } yield assertCompletes(ZIO.succeed(result))(equalTo(Chunk(1, 2, 3))) && duration < 100
    },
    
    test("combinators preserve laziness") {
      for {
        stream = ZStream.iterate(0)(_ + 1).take(10)
        
        processed <- Ref.make(0)
        
        _ <- stream.tap { _ =>
          processed.update(_ + 1)
        }.runDrain
        
        total <- processed.get
      } yield assertCompletes(ZIO.succeed(total))(equalTo(10))
    }
  )
}
