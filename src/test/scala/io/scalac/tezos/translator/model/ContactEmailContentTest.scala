package io.scalac.tezos.translator.model

import org.scalatest.{Matchers, WordSpec}

class ContactEmailContentTest extends WordSpec with Matchers {
  "EmailContentTest" should {
    "toJson" in {
      val dummyContactFormContent: EmailContent = ContactFormContent("Test", FullContact("+11223344", "test@service.com"), "TestContent")
      EmailContent.toJson(dummyContactFormContent) shouldBe """{"ContactFormContent":{"name":"Test","contact":{"FullContact":{"phone":"+11223344","email":"test@service.com"}},"content":"TestContent"}}"""
    }
    "fromJson" in {
      val expectedDummyContactFormContent: EmailContent = ContactFormContent("Test", FullContact("+11223344", "test@service.com"), "TestContent")
      val dummyTextContent = TextContent("""{"ContactFormContent":{"name":"Test","contact":{"FullContact":{"phone":"+11223344","email":"test@service.com"}},"content":"TestContent"}}""")
      EmailContent.fromJson(dummyTextContent.msg).get shouldBe expectedDummyContactFormContent
    }
    "toPrettyString" in {
      val dummyContactFormContent: EmailContent = ContactFormContent("Test", FullContact("+11223344", "test@service.com"), "TestContent")
      dummyContactFormContent.toString shouldBe """ContactFormContent(Test,FullContact(+11223344,test@service.com),TestContent)"""
    }
  }
}
