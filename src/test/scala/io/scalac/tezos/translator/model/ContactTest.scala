package io.scalac.tezos.translator.model
import Contact._
import org.scalatest.{Matchers, WordSpec}

class ContactTest extends WordSpec with Matchers {

  "ContactTest" should {
    "get Phone" in {
      val dummyContact = ContactPhone("+11223344")
      prettyString(dummyContact) shouldBe "phone: +11223344" + "\n"
    }
    "get Email" in {
      val dummyContact = ContactEmail(EmailAddress.fromString("test@service.com").get)
      prettyString(dummyContact) shouldBe "email: test@service.com" + "\n"
    }
    "get Both" in {
      val dummyContact = FullContact("+123", EmailAddress.fromString("test@service.com").get)
      prettyString(dummyContact) shouldBe "phone: +123\nemail: test@service.com\n"
    }
    "try To Create Full Filed Contact" in {
      val expectedDummyContact = FullContact("+123", EmailAddress.fromString("test@service.com").get)
      val assert = Contact.create(Option("+123"), Option(EmailAddress.fromString("test@service.com").get))
      assert.get shouldBe expectedDummyContact
    }
    "try To Create Contact with email" in {
      val expectedDummyContact = ContactEmail(EmailAddress.fromString("test@service.com").get)
      val assert = Contact.create(None, Option(EmailAddress.fromString("test@service.com").get))
      assert.get shouldBe expectedDummyContact
    }
    "try To Create Contact with phone" in {
      val expectedDummyContact = ContactPhone("+123")
      val assert = Contact.create(Some("+123"), None)
      assert.get shouldBe expectedDummyContact
    }
  }
}
