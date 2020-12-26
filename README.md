# exchange

A PoC to retrieve real-time Crypto Exchange data, as first step for a ordering bot!

## Run Application

A docker compose script is provided. Start the application with:

``
docker-compose --env-file env.dev up -d
``

Create your own *env.dev* file from the *env.template* file.

During development, the supported services (kafka, mongoDB) can be useful. For this a seperate docker-compose file is
created. Use it with:

``
docker-compose -f docker-compose-dev.yml --env-file env.dev up -d
``

## Test Performance
Using testcontainers can be slow during testing, because the used containers are removed after the test. The article 
[Reuse Containers With Testcontainers for Fast Integration Tests](https://rieckpil.de/reuse-containers-with-testcontainers-for-fast-integration-tests/)
tells how to make integration testing faster.

## TODO List

- ~~1 Exchange, 1 Currency pair connected~~
- ~~multiple services connected via Kafka~~
- ~~build pipeline~~
- ~~store events in database~~
- ~~multiple exchanges / multiple Currency pairs~~
- orderbook analysis module
- better frontend GUI
- logging via ELK stack  
- Binary messages in Kafka
- more robust error handling

## Related

- [Sonarcloud](https://sonarcloud.io/dashboard?id=buildingsoftwareblocks_exchange)
- [Docker hub](https://hub.docker.com/u/buildingsoftwareblocks)

## Background Information

- [Battle of the Bots: How Market Makers Fight It Out on Crypto Exchanges](https://medium.com/swlh/battle-of-the-bots-how-market-makers-fight-it-out-on-crypto-exchanges-2482eb937107)
- [known / XChange](https://github.com/knowm/XChange)
- [Using WebSocket to build an interactive web application](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Spring Websocket](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket-stomp-handle-send)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/docs/current/reference/html/#even-quicker-with-spring-boot)
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/#build-image)
