package io.scalac.tezos.translator.model
import Contact._
import io.scalac.tezos.translator.model.types.ContactData.{Phone, PhoneReq}
import org.scalatest.{Matchers, WordSpec}
import eu.timepit.refined.refineMV

class ContactTest extends WordSpec with Matchers {

  "ContactTest" should {
    "get Phone" in {
      val dummyContact = ContactPhone(Phone(refineMV[PhoneReq]("+11223344")))
      prettyString(dummyContact) shouldBe "phone: +11223344" + "\n"
    }
    "get Email" in {
      val dummyContact = ContactEmail(EmailAddress.fromString("test@service.com").get)
      prettyString(dummyContact) shouldBe "email: test@service.com" + "\n"
    }
    "get Both" in {
      val dummyContact = FullContact(Phone(refineMV[PhoneReq]("+123456")), EmailAddress.fromString("test@service.com").get)
      prettyString(dummyContact) shouldBe "phone: +123456\nemail: test@service.com\n"
    }
    "try To Create Full Filed Contact" in {
      val expectedDummyContact = FullContact(Phone(refineMV[PhoneReq]("+123456")), EmailAddress.fromString("test@service.com").get)
      val assert               = Contact.create(Option(Phone(refineMV[PhoneReq]("+123456"))), Option(EmailAddress.fromString("test@service.com").get))
      assert.get shouldBe expectedDummyContact
    }
    "try To Create Contact with email" in {
      val expectedDummyContact = ContactEmail(EmailAddress.fromString("test@service.com").get)
      val assert               = Contact.create(None, Option(EmailAddress.fromString("test@service.com").get))
      assert.get shouldBe expectedDummyContact
    }
    "try To Create Contact with phone" in {
      val expectedDummyContact = ContactPhone(Phone(refineMV[PhoneReq]("+123456")))
      val assert               = Contact.create(Some(Phone(refineMV[PhoneReq]("+123456"))), None)
      assert.get shouldBe expectedDummyContact
    }
  }
}
