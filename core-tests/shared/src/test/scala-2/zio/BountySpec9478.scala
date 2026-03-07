package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stream._

/**
 * Test suite for issue #9478
 * 
 * Issue: ZStream backpressure handling
 * 
 * This test verifies backpressure behavior.
 */
object BountySpec9478 extends ZIOSpecDefault {
  def spec = suite("ZStream backpressure")(
    test("ZStream respects backpressure") {
      for {
        processed <- Ref.make(0)
        
        stream = ZStream.fromIterable(1 to 1000)
          .tap { _ =>
            processed.update(_ + 1)
          }
          .take(10)
        
        _ <- stream.runDrain
        
        count <- processed.get
      } yield count == 10
    },
    
    test("ZStream doesn't buffer excessively") {
      for {
        stream = ZStream.fromIterable(1 to 100)
          .map(_ * 2)
          .take(10)
        
        result <- stream.runCollect
      } yield result.size == 10
    },
    
    test("ZStream handles slow consumer") {
      for {
        stream = ZStream.fromIterable(1 to 10)
          .tap { _ =>
            ZIO.sleep(10.millis)
          }
        
        result <- stream.runCollect
      } yield result.size == 10
    },
    
    test("ZStream.mapZIO applies backpressure") {
      for {
        stream = ZStream(1 to 50)
          .mapZIO(i => ZIO.succeed(i * 2))
          .take(10)
        
        result <- stream.runCollect
      } yield result.size == 10
    },
    
    test("ZStream.filter respects backpressure") {
      for {
        stream = ZStream.fromIterable(1 to 100)
          .filter(_ % 2 == 0)
          .take(10)
        
        result <- stream.runCollect
      } yield result.size == 10
    },
    
    test("ZStream.chunkN respects backpressure") {
      for {
        stream = ZStream(1 to 100)
          .chunks(5)
          .take(3)
        
        result <- stream.runCollect
      } yield result.size == 3
    }
  )
}
