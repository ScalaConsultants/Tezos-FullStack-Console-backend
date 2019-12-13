CONTAINER_ID=$1

docker stop $CONTAINER_ID
docker rm $CONTAINER_ID
rm -r ./postgres/data/
docker-compose -f ./docker-compose-local.yml up -d tezos-console-db
