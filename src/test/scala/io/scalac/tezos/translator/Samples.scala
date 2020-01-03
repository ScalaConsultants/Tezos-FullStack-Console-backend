package io.scalac.tezos.translator

object Samples {

  val micheline =
    """[
      |   {
      |      "prim":"parameter",
      |      "args":[
      |         {
      |            "prim":"int"
      |         }
      |      ]
      |   },
      |   {
      |      "prim":"storage",
      |      "args":[
      |         {
      |            "prim":"int"
      |         }
      |      ]
      |   },
      |   {
      |      "prim":"code",
      |      "args":[
      |         [
      |            {
      |               "prim":"CAR"
      |            },
      |            {
      |               "prim":"PUSH",
      |               "args":[
      |                  {
      |                     "prim":"int"
      |                  },
      |                  {
      |                     "int":"1"
      |                  }
      |               ]
      |            },
      |            {
      |               "prim":"ADD"
      |            },
      |            {
      |               "prim":"NIL",
      |               "args":[
      |                  {
      |                     "prim":"operation"
      |                  }
      |               ]
      |            },
      |            {
      |               "prim":"PAIR"
      |            }
      |         ]
      |      ]
      |   }
      |]""".stripMargin

  val incorrectMicheline1 =
    """
      |[
      |  {
      |    "prim": "ErrorHere",
      |    "args": [
      |      {
      |        "prim": "key_hash"
      |      }
      |    ]
      |  }
      |]""".stripMargin

  val incorrectMicheline2 =
    """
      |[
      |  {
      |    "prim": "ErrorHere",
      |    "args": [
      |      {
      |        "prim": "key_hash"
      |        """.stripMargin

  val michelson =
    """parameter int;
       |storage int;
       |code { CAR ;
       |       PUSH int 1 ;
       |       ADD ;
       |       NIL operation ;
       |       PAIR }""".stripMargin

  val incorrectMichelson1 =
    """parameter key_hash;
      |storage (pair key_hash timestamp);
      |code { DUP ;
      |       DIP { CDR } ;
      |       CAR ;
      |       { DIP { DUP } ; SWAP } ;
      |       DIP { DROP ; DROP } }""".stripMargin

  val incorrectMichelson2 = "bad string"

}
