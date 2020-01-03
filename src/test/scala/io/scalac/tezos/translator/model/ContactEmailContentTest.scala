package io.scalac.tezos.translator.model

import org.scalatest.{Matchers, WordSpec}

class ContactEmailContentTest extends WordSpec with Matchers {
  "EmailContentTest" should {
    "to Json and from " in {
      val dummyContactFormContent: EmailContent =
        ContactFormContent("Test", FullContact("+11223344", EmailAddress.fromString("test@service.com").get), "TestContent")

      EmailContent
        .toJson(dummyContactFormContent) shouldBe """{"ContactFormContent":{"name":"Test","contact":{"FullContact":{"phone":"+11223344","email":{"value":"test@service.com"}}},"content":"TestContent"}}"""

      EmailContent.fromJson(EmailContent.toJson(dummyContactFormContent)).get shouldBe dummyContactFormContent
    }
    "toPrettyString" in {
      val dummyContactFormContent: EmailContent =
        ContactFormContent("Test", FullContact("+11223344", EmailAddress.fromString("test@service.com").get), "TestContent")
      dummyContactFormContent.toString shouldBe """ContactFormContent(Test,FullContact(+11223344,test@service.com),TestContent)"""
    }
  }
}
