package io.scalac.tezos.translator.model

sealed trait Contact extends Product with Serializable {
  def getEmail(): String

  def getPhone(): String
}

case class ContactPhone(number: String) extends Contact {
  override def getEmail() = "Not declared"

  override def getPhone() = number
}

case class ContactEmail(email: String) extends Contact {
  override def getEmail() = email

  override def getPhone() = "Not declared"
}

case class FullContact(phone: String, email: String) extends Contact {
  override def getEmail() = email

  override def getPhone() = phone
}

object Contact {
  def tryToCreateContact(phone: String, email: String): Contact =
    (phone, email) match {
      case (phone, email) if !email.isEmpty && !phone.isEmpty => FullContact(phone, email)
      case (phone, _) if !phone.isEmpty() => ContactPhone(phone)
      case (_, email) if !email.isEmpty => ContactEmail(email)
      case _ => throw new Exception("Empty Email and Empty Phone Number")
    }
}
