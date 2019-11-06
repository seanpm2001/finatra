package com.twitter.finatra.tests.utils

import com.twitter.finatra.modules.CredentialsModule
import com.twitter.finatra.tests.utils.LocalFilesystemCredentialsIntegrationTest._
import com.twitter.finatra.test.LocalFilesystemTestUtils._
import com.twitter.finatra.utils.Credentials
import com.twitter.inject.IntegrationTest
import com.twitter.inject.app.TestInjector
import java.io.File

object LocalFilesystemCredentialsIntegrationTest {

  val CredentialsText: String =
    """
      |test_token: asdf
      |test_authorization_id: 123456
    """.stripMargin
}

class LocalFilesystemCredentialsIntegrationTest extends IntegrationTest {

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    // create keys/finatra directory and add credentials.yml
    val credentialsBasePath = createFile(s"${BaseDirectory}keys/finatra")
    writeStringToFile(createFile(credentialsBasePath, "credentials.yml"), CredentialsText)
  }

  override protected def afterAll(): Unit = {
    // try to help clean up
    new File(s"${BaseDirectory}keys").delete
    super.afterAll()
  }

  override val injector =
    TestInjector(
      flags = Map("credentials.file.path" -> s"${BaseDirectory}keys/finatra/credentials.yml"),
      modules = Seq(CredentialsModule)
    ).create

  test("load credentials") {
    val credentials = injector.instance[Credentials]
    credentials.isEmpty should be(false)

    credentials.get("test_token").get should be("asdf")
    credentials.get("test_authorization_id").get should be("123456")
  }
}
