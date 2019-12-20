export EMAIL_USER="mmt-info@scalac.io" \
export EMAIL_PASS=$1 \
export EMAIL_RECEIVER="mmt-admin-dev@scalac.io"

sbt clean assembly

docker build -t tezos-fullstack-console-backend .

docker-compose -f ./docker-compose-local.yml up -d