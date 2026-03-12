package zio.test

import zio.test.AssertionResult.Render
import zio.{ZIO, ZIOBaseSpec}

object AssertionVariantsSpec extends ZIOBaseSpec {
  def spec = suite("AssertionVariants")(
    test("equalTo should use Diff rendering for Product types") {
      case class Person(name: String, age: Int)
      val person1 = Person("Alice", 30)
      val person2 = Person("Alice", 31)

      // This should show detailed diff
      assertTrue(person1 == person2) // Should fail with nice diff
    },
    test("equalTo should use Diff rendering for collections") {
      val list1 = List(1, 2, 3)
      val list2 = List(1, 2, 4)

      // This should show detailed diff
      assertTrue(list1 == list2) // Should fail with nice diff
    },
    test("equalTo should work for equal values") {
      val value = 42
      assertTrue(value == 42) // Should pass
    }
  )
}
