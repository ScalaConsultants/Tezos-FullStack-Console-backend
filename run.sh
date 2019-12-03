docker run -i --rm \
    --name tezos-fullstack-console-backend \
    -v "$(pwd):/src" \
    -w /src \
    hseeberger/scala-sbt:8u181_2.12.8_1.2.8 \
    sbt -Dsbt.global.base=/.sbt/global \
      -Dsbt.boot.directory=/.sbt/boot/ \
      -Dsbt.coursier.home=/.sbt/coursier/ clean assembly

docker build -t tezos-fullstack-console-backend .

docker-compose -f ./docker-compose-local.yml up -d