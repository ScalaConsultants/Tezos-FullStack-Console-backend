package io.scalac.tezos.translator.model

import org.scalatest.{Matchers, WordSpec}

class ContactTest extends WordSpec with Matchers {

  "ContactTest" should {
    "get Phone" in {
      val dummyContact = ContactPhone("+11223344")
      dummyContact.getPhone() shouldBe "+11223344"
      dummyContact.getEmail() shouldBe "Not declared"
    }
    "get Email" in {
      val dummyContact = ContactEmail("test@service.com")
      dummyContact.getEmail() shouldBe "test@service.com"
      dummyContact.getPhone() shouldBe "Not declared"
    }
    "get Both" in {
      val dummyContact = FullContact("+123", "test@service.com")
      dummyContact.getEmail() shouldBe "test@service.com"
      dummyContact.getPhone() shouldBe "+123"
    }
    "tryToCreateContact" in {
      val expectedDummyContact = FullContact("+123", "test@service.com")
      val assert = Contact.tryToCreateContact("+123", "test@service.com")
      assert shouldBe expectedDummyContact
    }

  }
}
