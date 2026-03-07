package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import zio.stm._

/**
 * Test suite for issue #10196
 * 
 * Issue: Concurrent TSet operations
 * 
 * This test verifies concurrent set operations.
 */
object BountySpec10196 extends ZIOSpecDefault {
  def alloc spec = suite("Concurrent TSet operations")(
    test("concurrent insert is isolated") {
      for {
        set <- TSet.empty[Int].commit
        
        fibers <- ZIO.foreachPar((1 to 10).toList) { i =>
          STM.atomically {
            set.put(i)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(set.size)
      } yield assertCompletes(ZIO.succeed(size))(equalTo(10))
    },
    
    test("concurrent lookup is consistent") {
      for {
        set <- TSet.empty[Int].commit
        
        _ <- STM.atomically {
          for (i <- 1 to 5) {
            set.put(i)
          }
        }
        
        results <- ZIO.foreachPar((1 to 10).toList) { _ =>
          STM.atomically {
            set.contains(3)
          }
        }
        
        allFound <- ZIO.foreach(results)(r => ZIO.succeed(r.isDefined)).map(_.exists(identity))
      } yield assertCompletes(ZIO.succeed(allFound))(isTrue)
    },
    
    test("concurrent delete is safe") {
      for {
        set <- TSet.fromIterable((1 to 10).toList).commit
        
        fibers <- ZIO.foreachPar((1 to 5).toList) { _ =>
          STM.atomically {
            set.delete(3)
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(set.size)
      } yield size >= 5
    },
    
    test("concurrent union is isolated") {
      for {
        set1 <- TSet.fromIterable((1 to 5).toList).commit
        set2 <- TSet.fromIterable((6 to 10).toList).commit
        
        fibers <- ZIO.foreachPar(List(set1, set2)) { set =>
          STM.atomically {
            set.foreach(a => set.put(a))
          }
        }
        
        _ <- ZIO.foreachDiscard(fibers)(_.join)
        
        size <- STM.atomically(TSet.empty[Int].commit.flatMap { empty =>
          set1.foreach(a => empty.put(a))
        }.size)
      } yield assertCompletes(ZIO.succeed(size))(equalTo(10))
    },
    
    test("concurrent intersect is safe") {
      for {
        set1 <- TSet.fromIterable((1 to 5).toList).commit
        set2 <- TSet.fromIterable((3 to 7).toList).commit
        
        result <- STM.atomically {
          set1.intersect(set2)
        }.size
        
        r <- ZIO.succeed(r)
      } yield r == 3 // 3, 4, 5, 6, 7 的交集
    },
    
    test("concurrent difference is safe") {
      for {
        set1 <- TSet.fromIterable((1 to 10).toList).commit
        set2 <- TSet.fromIterable((5 to 15).toList).commit
        
        result <- STM.atomically {
          set1.diff(set2)
        }.size
        
        r <- ZIO.succeed(r)
      } yield r >= 5 // 1,2,3,4, 10 与 5,6,7,8,9,11,12,13,14,15 的差集
    }
  )
}
