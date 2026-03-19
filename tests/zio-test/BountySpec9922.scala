package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for ZStreamAspect
 * Auto-generated for bounty claim #9922
 */
object BountySpec9922 extends ZIOSpecDefault {
  def spec = suite("ZStreamAspect tests")(
    test("ZStreamAspect identity preserves effects") {
      val aspect = ZStreamAspect.identity
      val stream = ZStream(1, 2, 3)
      assertCompletes(stream.apply(aspect).toList)(equalTo(List(1, 2, 3)))
    },
    
    test("ZStreamAspect.and combines aspects") {
      val aspect1 = ZStreamAspect.identity
      val aspect2 = ZStreamAspect.identity
      val combined = aspect1.and(aspect2)
      val stream = ZStream(1)
      assertCompletes(stream.apply(combined).toList)(equalTo(List(1)))
    },
    
    test("ZStreamAspect.before adds preprocessing") {
      var ran = false
      val aspect = ZStreamAspect.before(ZIO.succeed(ran = true))
      val stream = ZStream(1)
      assertCompletes(stream.apply(aspect).runDrain *> ZIO.succeed(ran))(isTrue)
    }
  )
}
