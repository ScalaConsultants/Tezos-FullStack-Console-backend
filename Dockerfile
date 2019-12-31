FROM openjdk:11

RUN apt-get install -y curl \
  && curl -sL https://deb.nodesource.com/setup_9.x | bash - \
  && apt-get install -y nodejs \
  && curl -L https://www.npmjs.com/install.sh | sh

RUN mkdir -p /opt/app
WORKDIR /opt/app

RUN npm i nearley@2.16.0

COPY ./wait-for-it.sh /usr/wait-for-it.sh
RUN chmod +x /usr/wait-for-it.sh

EXPOSE 8080

COPY ./target/scala-2.12/tezos-console.jar ./
