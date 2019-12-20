package io.scalac.tezos.translator

object Helper {
   def testFormat(s: String): String = s.replaceAll("\n", "").replaceAll("\r", "")
}
