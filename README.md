# Tezos FullStack Console

REST API for translating from `Michelson` to `Micheline` and back.
It utilizes [Tezos-FullStack-Console-Translation-Module](https://github.com/ScalaConsultants/Tezos-FullStack-Console-Translation-Module) to perform it.

## Prerequisites

* JDK (>= 8.x)
* Scala (> 2.12.8)
* SBT (> 1.2.8)

For running it in a container:
* Docker

For running it without a container:
* Node.js with nearly ([Tezos-FullStack-Console-Translation-Module](https://github.com/ScalaConsultants/Tezos-FullStack-Console-Translation-Module) requires it)
* PostgreSQL database

## Running it in a container

1. Clone repo
2. Have Docker service running
3. Execute `./run.sh`

## Running tests

* For local unit tests use `sbt test`
* For full integration tests use (with Docker service running) `sbt testAll`

## Running it without container

1. Clone repo
2. Run `npm install`
3. Configure PostgreSQL access in `application.conf`
4. Run `sbt run`

## Other information about development environment

* Make sure the `./run.sh` script has execution permissions.

* For working CORS the `FE_URL` and `BE_URL` environmental variables need to be correct.
For local development they are already set up in the `./run.sh` script (`localhost:3000` for frontend, `localhost:30002` for backend).
When desirable, change their values in that file.

* For running the application with working mailing, one need to update `EMAIL_PASS` with a proper value (or provide other account for the service to use).
```./run.sh $EMAIL_PASS```

* Alternatively only the DB container can be run with `docker-compose up -d tezos-console-db` and the application run with:
```
TEZOS_DB_IP="127.0.0.1" \
sbt run
```

* Backend documentation is provided by Swagger and it is available at `$BE_URL/docs`

## Exemplary queries

Backed is available at `localhost` at port `8080` if running without a container
or at port `30002` if running with a container.
Query your local instance with exemplary snippets:

* From `Michelson` to `Micheline`:

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

* From `Micheline` to `Michelson`:

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

## References

* [Conseil](https://github.com/Cryptonomic/Conseil)

* [ConseilJS](https://github.com/Cryptonomic/ConseilJS)
