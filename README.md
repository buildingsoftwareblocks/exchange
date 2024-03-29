![Develop](https://github.com/buildingsoftwareblocks/exchange/workflows/Develop/badge.svg)
![Main](https://github.com/buildingsoftwareblocks/exchange/workflows/Main/badge.svg)

# Exchange Data PoC

A PoC to retrieve real-time Crypto Exchange data, as a first step for an ordering bot! The code belongs to this
[YouTube sequence](https://www.youtube.com/playlist?list=PLQkCUEPgDgc1dItDlEjQ-sTXjY7kR-76z).

## System Overview

![System Overview](system-overview.png)

## Run Application

### Docker Compose

A docker compose script is provided. Start the application with:

``docker compose --profile full up -d``

With scaling:

``docker compose --profile full up --scale frontend=2 --scale backend=2 --scale analysis=2 -d``

The profiles can be used in every call `--profile=full` for example, or by setting the environment variable
*COMPOSE_PROFILES*.

For example in Window/Powershell: `$Env:COMPOSE_PROFILES="full"`.

### Kubernetes

...

### Links

| System        | link                   |
|---------------|------------------------|
| frontend      | http://localhost:8081/ |
| Kafka console | http://localhost:8181/ | 

## Test Performance

Using testcontainers can be slow during testing, because the used containers are removed after the test. The article
[Reuse Containers With Testcontainers for Fast Integration Tests](https://rieckpil.de/reuse-containers-with-testcontainers-for-fast-integration-tests/)
tells how to make integration testing faster.

## MongoDB queries

The following queries show some useful information:

Number of records:

````mongodb
db.message.count()
````

Date values :

````mongodb
db.message.aggregate(
    [
        {
            $group:{
                _id: {
                    exchange: "$exchange"
                },
                "min date": {
                    $min : "$created"
                },
                "max date" : {
                    $max : "$created"
                },
                "#messages" : {
                    $sum:1
                }
            }
        }
    ]
    )
````

Message sizes:

````mongodb
db.message.aggregate(
    [
        {
            $project: {
                currencyPair: 1,
                exchange: 1,
                length: {
                    $strLenCP: "$message"
                }
            }
        },
        {
            $group:{
                _id: {
                    exchange: "$exchange",
                    currencypair:"$currencyPair"
                },
                "avg message" : {
                    $avg : "$length"
                },
                "max message" : {
                    $max : "$length"
                },
                "min message" : {
                    $min : "$length"
                },
                "#messages" : {
                    $sum:1
                },
                "volume" : {
                    $sum : "$length"
                }
            }
        }
    ]
    )
````

## Build Application

You can build the application locally via a [maven](https://maven.apache.org/) command:

| target                                | meaning                                         |
|---------------------------------------|-------------------------------------------------|
| mvn clean                             | clean environment                               |
| mvn install                           | build self executable JAR files                 |
| mvn install -P docker                 | build docker images                             |
| mvn versions:display-property-updates | check if latest versions of libraries are used. |

To use the created local docker image, replace ``ghcr.io/buildingsoftwareblocks/``with``local``in
the used *docker-compose.yml* file.

To make sure that you have the latest images, use: ``docker compose --profile=full pull``.

The profiles can be used in every call ``--profile=full`` for example, or by setting the environment variable
*COMPOSE_PROFILES*.

For example in Window/Powershell: ``$Env:COMPOSE_PROFILES="full"``.

## TODO List

- [x] 1 Exchange, 1 Currency pair connected
- [x] multiple services connected via Kafka
- [x] build pipeline
- [x] store events in database
- [x] multiple exchanges / multiple Currency pairs
- [x] order book analysis module
- [x] simple arbitrage
- [x] scalable backend module
- [x] scalable frontend module
- [x] scalable docker frontend
- [x] scalable analysis module
- [x] better logging
- [ ] triangular Arbitrage
- [x] better frontend GUI
- [ ] binary messages in Kafka
- [x] more robust error handling
- [ ] run on Kubernetes
- [ ] ElasticSearch to version 8 (SB3 supported version)

## Related

- [Sonar cloud](https://sonarcloud.io/dashboard?id=buildingsoftwareblocks_exchange)

## Background Information

- [Battle of the Bots: How Market Makers Fight It Out on Crypto Exchanges](https://medium.com/swlh/battle-of-the-bots-how-market-makers-fight-it-out-on-crypto-exchanges-2482eb937107)
- [known / XChange](https://github.com/knowm/XChange)
- [Using WebSocket to build an interactive web application](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Spring Websocket](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#websocket-stomp-handle-send)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/docs/current/reference/html/#even-quicker-with-spring-boot)
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/#build-image)
- [The Easy Cryptocurrency Arbitrage Trading Strategies](https://blog.shrimpy.io/blog/cryptocurrency-arbitrage-a-lucrative-trading-strategy)
