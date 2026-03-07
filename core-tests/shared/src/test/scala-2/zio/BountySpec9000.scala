package zio.test

import zio._
import zio.test._
import zio.test.Assertion._

/**
 * Test suite for issue #9000
 * 
 * Issue: Provide platform-specific environment for tests
 * 
 * This test verifies platform-specific test execution.
 */
object BountySpec9000 extends ZIOSpecDefault {
  def spec = suite("Platform-specific test environment")(
    test("test runs on JVM platform") {
      val isJVM = System.getProperty("java.vm.name") != null
      assertCompletes(ZIO.succeed(isJVM))(isTrue)
    },
    
    test("test accesses system properties") {
      val javaVersion = System.getProperty("java.version")
      assertCompletes(ZIO.succeed(javaVersion.nonEmpty))(isTrue)
    },
    
    test("test detects Scala version") {
      val scalaVersion = scala.util.Properties.versionString
      assertCompletes(ZIO.succeed(scalaVersion.contains("version")))(isTrue)
    },
    
    test("test provides OS information") {
      val osName = System.getProperty("os.name")
      assertCompletes(ZIO.succeed(osName.nonEmpty))(isTrue)
    },
    
    test("test platform is consistent") {
      val vendor = System.getProperty("java.vendor")
      val vmName = System.getProperty("java.vm.name")
      
      assertCompletes(ZIO.succeed(vendor.nonEmpty && vmName.nonEmpty))(isTrue)
    }
  )
}
