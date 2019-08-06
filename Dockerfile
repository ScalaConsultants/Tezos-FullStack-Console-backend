FROM openjdk:11

RUN apt-get install -y curl \
  && curl -sL https://deb.nodesource.com/setup_9.x | bash - \
  && apt-get install -y nodejs \
  && curl -L https://www.npmjs.com/install.sh | sh

RUN mkdir -p /opt/app
WORKDIR /opt/app

RUN npm i nearley

EXPOSE 8080

COPY ./target/scala-2.12/console.jar ./

CMD java -jar console.jar