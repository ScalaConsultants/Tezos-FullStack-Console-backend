package io.scalac.tezos.translator.model

import javax.mail.internet.InternetAddress

import scala.util.Try

sealed trait Contact extends Product with Serializable{
  def  getEmail():String
  def  getPhone():String
}
case class PhoneNumber(number: String) extends Contact {
  override  def  getEmail()="Not Declared"
  override def  getPhone() = number
}
case class Email(email: String) extends Contact {
  override  def  getEmail()=email
  override def  getPhone() ="Not Declared"
}
case class FullFilledContact(phone: String, email: String) extends Contact {
  override  def  getEmail()=email
  override def  getPhone() = phone
}


object Contact {

def tryToCreateContact(phone:String,email:String) : Contact=
  (phone, email) match {
    case (phone, email) if !email.isEmpty && !phone.isEmpty => FullFilledContact(phone, email)
    case (phone, _) if !phone.isEmpty() => PhoneNumber(phone)
    case (_, email) if !email.isEmpty => Email(email)
    case _ => throw  new Exception("Empty Email and Empty Phone Number")
  }
}
