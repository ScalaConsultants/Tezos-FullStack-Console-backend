package io.scalac.tezos.translator.model

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