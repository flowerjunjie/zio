package zio.test

import zio._
import zio.test._
import zio.test.Assertion._
import scala.collection.mutable

/**
 * Test suite for issue #9189
 * 
 * Issue: Use ConcurrentLinkedDeque from Scala Native
 * 
 * This test verifies deque behavior for Scala Native compatibility.
 */
object BountySpec9189 extends ZIOSpecDefault {
  def spec = suite("ConcurrentLinkedDeque for Scala Native")(
    test("ConcurrentLinkedDeque supports concurrent add/remove") {
      val deque = new java.util.concurrent.ConcurrentLinkedDeque[Int]()
      
      // Add elements
      (1 to 10).foreach(deque.addLast)
      
      // Remove from both ends
      val first = deque.pollFirst()
      val last = deque.pollLast()
      
      assertCompletes(ZIO.succeed((first, last)))(equalTo((1, 10)))
    },
    
    test("ConcurrentLinkedDeque is thread-safe") {
      val deque = new java.util.concurrent.ConcurrentLinkedDeque[Int]()
      
      // Concurrent additions
      fibers <- ZIO.foreachPar((1 to 5).toList) { i =>
        ZIO.succeed(deque.addLast(i))
      }.fork
      
      _ <- fibers.join
      
      // Verify all elements added
      size = deque.size()
      
      assertCompletes(ZIO.succeed(size))(equalTo(5))
    },
    
    test("ConcurrentLinkedDeque supports peek operations") {
      val deque = new java.util.concurrent.ConcurrentLinkedDeque[Int]()
      
      deque.addLast(1)
      deque.addLast(2)
      deque.addLast(3)
      
      val first = deque.peekFirst()
      val last = deque.peekLast()
      
      assertCompletes(ZIO.succeed((first, last)))(equalTo((1, 3)))
    },
    
    test("ConcurrentLinkedDeque handles empty deque") {
      val deque = new java.util.concurrent.ConcurrentLinkedDeque[Int]()
      
      val first = deque.peekFirst()
      val last = deque.peekLast()
      val polledFirst = deque.pollFirst()
      
      assertCompletes(ZIO.succeed((first, last, polledFirst)))(equalTo((null, null, null)))
    },
    
    test("ConcurrentLinkedDeque maintains order") {
      val deque = new java.util.concurrent.ConcurrentLinkedDeque[Int]()
      
      val elements = (1 to 5).toList
      elements.foreach(deque.addLast)
      
      // Iterate and verify order
      val collected = mutable.ListBuffer[Int]()
      val iterator = deque.iterator()
      while (iterator.hasNext) {
        collected += iterator.next()
      }
      
      assertCompletes(ZIO.succeed(collected.toList))(equalTo(elements))
    }
  )
}
