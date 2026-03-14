package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #10086
 * 
 * Issue: Concurrent Map operations
 * 
 * This test verifies concurrent map operation safety.
 */
object BountySpec10086 extends ZIOSpecDefault {
  def spec = suite("Concurrent map operations")(
    test("concurrent TMap operations are isolated") {
      for {
        map <- TMap.empty[String, Int].commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          STM.atomically {
            map.put(s"key$i", i)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(map.size)
      } yield assertCompletes(ZIO.succeed(size))(equalTo(10))
    },
    
    test("concurrent TMap lookups don't cause race conditions") {
      for {
        map <- TMap.empty[String, Int].commit
        
        # Pre-populate
        _ <- STM.atomically {
          for (i <- 1 to 10) {
            map.put(s"key$i", i * 10)
          }
        }
        
        results <- ZIO.foreachPar((1 to 20).toList) { i =>
          STM.atomically {
            map.get(s"key${i % 10 + 1}")
          }
        }
        
        allFound = results.forall(_.isDefined)
      } yield assertCompletes(ZIO.succeed(allFound))(isTrue)
    },
    
    test("concurrent TMap deletions are safe") {
      for {
        map <- TMap.empty[String, Int].commit
        
        _ <- STM.atomically {
          for (i <- 1 to 10) {
            map.put(s"key$i", i)
          }
        }
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { i =>
          STM.atomically {
            map.delete(s"key{i}")
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(map.size)
      } yield size >= 5
    },
    
    test("concurrent TMap updates don't lose data") {
      for {
        map <- TMap.make[String, Int]("init", 0).commit
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { i =>
          STM.atomically {
            map.update(s"value", i * 2)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        value <- STM.atomically(map.get("value"))
      } yield assertCompletes(ZIO.succeed(value))(exists(_.isDefined))
    },
    
    test("concurrent TMap collectAll works") {
      for {
        map <- TMap.empty[String, Int].commit
        
        _ <- STM.atomically {
          for (i <- 1 to 10) {
            map.put(s"key$i", i)
          }
        }
        
        result <- STM.atomically {
          map.collect
        }
        
        size = result.size
      } yield size == 10
    }
  )
}
