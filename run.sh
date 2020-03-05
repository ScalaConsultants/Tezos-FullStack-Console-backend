export EMAIL_USER="mmt-info@scalac.io"
export EMAIL_PASS="$1"
export EMAIL_RECEIVER="mmt-admin-dev@scalac.io"

export FE_URL="http://localhost:3000"
export BE_URL="http://localhost:30002"

sbt clean assembly

docker build -t tezos-fullstack-console-backend .

docker-compose -f ./docker-compose-local.yml up -d
