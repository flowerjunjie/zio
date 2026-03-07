package zio

import java.util.concurrent.ConcurrentLinkedQueue

private[zio] trait QueuePlatformSpecific {

  // Use native ConcurrentLinkedDeque if available (Scala Native 0.5.6+),
  // otherwise fall back to custom implementation
  private[zio] final class ConcurrentDeque[A <: AnyRef] extends ConcurrentLinkedQueue[A] {

    def addFirst(a: A): Unit = {
      var popped = poll()
      if (popped eq null) {
        offer(a)
      } else {
        val buf = new scala.collection.mutable.ArrayBuffer[A]
        while (popped ne null) {
          buf += popped
          popped = poll()
        }
        offer(a)
        buf.foreach(offer)
      }
    }

  }
}
