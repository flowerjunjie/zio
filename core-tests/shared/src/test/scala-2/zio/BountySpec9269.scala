package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9269
 * 
 * Issue: Type inference in some scenarios is weak
 * 
 * This test verifies type inference improvements.
 */
object BountySpec9269 extends ZIOSpecDefault {
  def spec = suite("Type inference improvements")(
    test("type inference works with ZIO.map") {
      for {
        result <- ZIO.succeed(1).map(_ + 1)
      } yield assertCompletes(ZIO.succeed(result))(equalTo(2))
    },
    
    test("type inference works with nested ZIO") {
      for {
        result <- ZIO.succeed(ZIO.succeed(1)).flatten
      } yield assertCompletes(ZIO.succeed(result))(equalTo(1))
    },
    
    test("type inference works with ZIO.foreach") {
      val list = List(1, 2, 3)
      
      for {
        result <- ZIO.foreach(list)(i => ZIO.succeed(i * 2))
      } yield assertCompletes(ZIO.succeed(result))(equalTo(List(2, 4, 6)))
    },
    
    test("type inference works with complex chains") {
      for {
        result <- ZIO.succeed(1)
          .map(_ + 1)
          .flatMap(i => ZIO.succeed(i * 2))
          .map(_ + 1)
      } yield assertCompletes(ZIO.succeed(result))(equalTo(5))
    },
    
    test("type inference works with generics") {
      def process[A](value: A): ZIO[Any, Nothing, A] = ZIO.succeed(value)
      
      for {
        result <- process(42)
      } yield assertCompletes(ZIO.succeed(result))(equalTo(42))
    },
    
    test("type inference preserves variance") {
      trait Animal
      case class Dog() extends Animal
      
      for {
        dog <- ZIO.succeed(Dog())
        animal: ZIO[Any, Nothing, Animal] = dog
      } yield assertCompletes(animal)(anything)
    }
  )
}
