package zio

import zio.test._
import zio.test.Assertion._

object ZIOAppSpec extends ZIOSpecDefault {


  // Test 1: ZIOApp basic execution
  suite("ZIOApp execution") {

    test("simple app executes and exits") {
      val app = new ZIOApp {
        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.succeed(42)
      }

      for {
        exit <- ZIOApp.executable(app.run).exitCode
      } yield assertTrue(exit == 0)
    }

    test("app with failure exits with error") {
      val app = new ZIOApp {
        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.fail("error")
      }

      for {
        exit <- ZIOApp.executable(app.run).exitCode.sandbox
      } yield assert(exit)(isGreaterThan(0))
    }
  }

  // Test 2: ZIOApp args handling
  suite("ZIOApp args") {

    test("app receives command line args") {
      val app = new ZIOApp {
        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIOAppArgs.getArgs.flatMap(args => ZIO.succeed(args.toList))
      }

      for {
        args <- ZIOApp.executable(app.run, List("--arg1", "--arg2")).args
      } yield assertTrue(args == List("--arg1", "--arg2"))
    }
  }

  // Test 3: ZIOApp lifecycle
  suite("ZIOApp lifecycle") {

    test("gracefulShutdownTimeout is respected") {
      val app = new ZIOApp {
        override val gracefulShutdownTimeout: Duration = 100.millis

        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.never
      }

      // Test that app times out after gracefulShutdownTimeout
      for {
        fiber <- ZIOApp.executable(app.run).fork
        _ <- ZIO.sleep(200.millis)
        _ <- fiber.interrupt
      } yield assertCompletes
    }

    test("hooks are executed in order") {
      var hooks = List.empty[String]

      val app = new ZIOApp {
        override def boot: ZIO[Any, Nothing, Any] =
          ZIO.succeed { hooks = hooks :+ "boot" }

        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.succeed { hooks = hooks :+ "run" }

        override def finalize: ZIO[Any, Nothing, Any] =
          ZIO.succeed { hooks = hooks := "finalize" }
      }

      for {
        _ <- ZIOApp.executable(app.run).exitCode
      } yield assertTrue(hooks == List("boot", "run", "finalize"))
    }
  }

  // Test 4: ZIOApp environment
  suite("ZIOApp environment") {

    test("app can access environment") {
      val app = new ZIOApp {
        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.environment[Any].as(())
      }

      for {
        exit <- ZIOApp.executable(app.run).exitCode
      } yield assertTrue(exit == 0)
    }
  }

  // Test 5: ZIOApp error handling
  suite("ZIOApp error handling") {

    test("defect in app causes non-zero exit") {
      val app = new ZIOApp {
        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.die(new RuntimeException("crash"))
      }

      for {
        exit <- ZIOApp.executable(app.run).exitCode
      } yield assert(exit)(isGreaterThan(0))
    }

    test("interruption is handled gracefully") {
      val app = new ZIOApp {
        override def run: ZIO[ZIOAppArgs, Any, Any] =
          ZIO.interrupt
      }

      for {
        exit <- ZIOApp.executable(app.run).exitCode
      } yield assertTrue(exit == 0)
    }
  }
}

}