docker run -i --rm \
    --name tezos-fullstack-console-backend \
    -v "$(pwd):/src" \
    -w /src \
    hseeberger/scala-sbt:8u222_1.3.4_2.12.10 \
    sbt clean assembly

docker build -t tezos-fullstack-console-backend .

docker-compose -f ./docker-compose-local.yml up -d