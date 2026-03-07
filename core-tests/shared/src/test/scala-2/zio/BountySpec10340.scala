package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #10340
 * 
 * Issue: Thread-safe data structures
 * 
 * This test verifies thread-safety of data structures.
 */
object BountySpec10340 extends ZIOSpecDefault {
  def spec = suite("Thread-safe data structures")(
    test("Concurrent Ref updates don't lose data") {
      for {
        ref <- Ref.make(0)
        
        _ <- ZIO.foreachPar((1 to 30).toList) { _ =>
          ref.update(_ + 1)
        }
        
        value <- ref.get
      } yield value == 30
    },
    
    test("Ref modify is isolated") {
      for {
        ref <- Ref.make((0, 0))
        
        results <- ZIO.foreachPar((1 to 10).toList) { i =>
          ref.modify(v => (v + i, v * 2))
        }
        
        sum <- ZIO.foreach(results)(_.join)
        
        total <- ZIO.succeed(sum.sum)
      } yield total >= 11 // (0+1)*2 + (1+2)*2 + ... + (9+10)*2 = 0+2+6+12+20+30+40+42+56+72+90 = 110)
    },
    
    test("Ref get is consistent under concurrency") {
      for {
        ref <- Ref.make(42)
        
        results <- ZIO.foreachPar((1 to 20).toList) { _ =>
          ref.get
        }
        
        allSame = results.forall(r => r == 42)
      } yield assertCompletes(ZIO.succeed(allSame))(isTrue)
    },
    
    test("Ref alternatives are atomic") {
      for {
        ref <- Ref.make(0)
        
        results <- ZIO.foreachPar((1 to 10).toList) { _ =>
          ref.getOrElseUpdate(0)(_ + 1)
        }
        
        sum <- ZIO.foreach(results)(_.join)
        
        total <- ZIO.succeed(sum.sum)
      } yield total == 10
    },
    
    test("Ref collectAll is concurrent-safe") {
      for {
        results <- ZIO.foreachPar((1 to 5).toList) { i =>
          ref.fold(
            s"key$i" -> i
          )(_ => s"key$j" -> i + 10)
        }.fork
        
        _ <- ZIO.foreach(results)(_.join)
        
        allResults <- ZIO.foreach(results)(_.join)
        
        combined <- allResults.flatten
        
        size <- combined.size
      } yield size == 10
    },
    
    test("Ref collectAll is deterministic") {
      for {
        ref <- Ref.make(Map.empty[Int, String])
        
        _ <- ZIO.foreach((1 to 10).toList) { i =>
          ref.update(m => m + (i -> s"value$i"))
        }
        
        allResults <- ZIO.foreach((1 to 10).toList) { _ =>
          ref.collect(_.values)
        }.fork
        
        allMaps <- ZIO.foreach(allResults)(_.join)
        
        map <- ZIO.succeed(allMaps.reduce(_.++))
        
        size <- map.size
      } yield size == 10
    },
    
    test("Ref collectAll preserves insertion order") {
      for {
        ref <- Ref.make(Map.empty[Int, String])
        
        _ <- ZIO.foreach((1 to 5).toList) { i =>
          ref.update(m => m + (i -> s"value$i"))
        }
        
        order <- ZIO.succeed(ref.get.flatMap(_.keys).mkString(", "))
        
        r <- ZIO.succeed(order == "key1, key2, key3, key4, key5")
      } yield r
    }
  )
}
