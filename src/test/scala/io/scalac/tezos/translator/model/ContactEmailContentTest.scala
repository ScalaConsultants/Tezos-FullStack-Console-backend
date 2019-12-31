package io.scalac.tezos.translator.model

import eu.timepit.refined.collection.NonEmpty
import io.scalac.tezos.translator.model.types.ContactData._
import org.scalatest.{Matchers, WordSpec}
import eu.timepit.refined.refineMV

class ContactEmailContentTest extends WordSpec with Matchers {
  "EmailContentTest" should {
    "to Json and from " in {
      val dummyContactFormContent: EmailContent = ContactFormContent(
        Name(refineMV[NameReq]("Test")),
        FullContact(Phone(refineMV[PhoneReq]("+11223344")),
        EmailAddress.fromString("test@service.com").get),
        Content(refineMV[NonEmpty]("TestContent")))

      EmailContent.toJson(dummyContactFormContent) shouldBe """{"ContactFormContent":{"name":"Test","contact":{"FullContact":{"phone":"+11223344","email":{"value":"test@service.com"}}},"content":"TestContent"}}"""

      EmailContent.fromJson(EmailContent.toJson(dummyContactFormContent)).get shouldBe dummyContactFormContent
    }
    "toPrettyString" in {
      val dummyContactFormContent: EmailContent = ContactFormContent(
        Name(refineMV[NameReq]("Test")),
        FullContact(Phone(refineMV[PhoneReq]("+11223344")),
        EmailAddress.fromString("test@service.com").get),
        Content(refineMV[NonEmpty]("TestContent")))
      dummyContactFormContent.toString shouldBe """ContactFormContent(Test,FullContact(+11223344,test@service.com),TestContent)"""
    }
  }
}
