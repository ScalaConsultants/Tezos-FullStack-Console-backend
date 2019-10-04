# Tezos FullStack Console

REST API for translating from `Michelson` to `Micheline` and back.
It utilizes [Tezos-FullStack-Console-Translation-Module](https://github.com/ScalaConsultants/Tezos-FullStack-Console-Translation-Module) to perform it.

## Prerequisites
* JDK (>= 8.x)
* Scala (> 2.12.8)
* SBT (> 1.2.8)
* Node.js with nearly ([Tezos-FullStack-Console-Translation-Module](https://github.com/ScalaConsultants/Tezos-FullStack-Console-Translation-Module) requires it)
* mysql database

## Usage

1. Clone repo

2. Run `npm install`

3. Configure mysql access in `application.conf`

4. Run `sbt run`

5. Query your local instance with exemplary snippets

From `Michelson` to `Micheline`:

```
POST http://localhost:8080/v1/translate/from/michelson/to/micheline
Content-Type: text/plain

parameter int;
storage int;
code { CAR ;
       PUSH int 1 ;
       ADD ;
       NIL operation ;
       PAIR }
```

From `Micheline` to `Michelson`:

```
POST http://localhost:8080/v1/translate/from/micheline/to/michelson
Content-Type: application/json

[
  {
    "prim": "parameter",
    "args": [
      {
        "prim": "int"
      }
    ]
  },
  {
    "prim": "storage",
    "args": [
      {
        "prim": "int"
      }
    ]
  },
  {
    "prim": "code",
    "args": [
      [
        {
          "prim": "CAR"
        },
        {
          "prim": "PUSH",
          "args": [
            {
              "prim": "int"
            },
            {
              "int": "1"
            }
          ]
        },
        {
          "prim": "ADD"
        },
        {
          "prim": "NIL",
          "args": [
            {
              "prim": "operation"
            }
          ]
        },
        {
          "prim": "PAIR"
        }
      ]
    ]
  }
]
```

##Run in docker

```sbt assembly```

```docker-compose up```

## References

[Conseil](https://github.com/Cryptonomic/Conseil)

[ConseilJS](https://github.com/Cryptonomic/ConseilJS)
