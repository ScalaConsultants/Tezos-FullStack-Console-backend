export EMAIL_USER="mmt-info@scalac.io"
export EMAIL_PASS=""
export EMAIL_RECEIVER="mmt-admin-dev@scalac.io"

docker run -i --rm \
    --name tezos-fullstack-console-backend \
    -v "$(pwd):/src" \
    -w /src \
    hseeberger/scala-sbt:11.0.4_1.3.4_2.12.10 \
    sbt clean assembly

docker build -t tezos-fullstack-console-backend .

docker-compose -f ./docker-compose-local.yml up -d