FROM openjdk:17 as builder
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM bellsoft/liberica-openjdk-alpine:17
VOLUME /tmp
RUN addgroup -S demo && adduser -S demo -G demo
USER demo
COPY --from=builder dependencies/ ./
RUN true # This is a workaround to https://github.com/moby/moby/issues/37965
COPY --from=builder snapshot-dependencies/ ./
RUN true # This is a workaround to https://github.com/moby/moby/issues/37965
COPY --from=builder spring-boot-loader/ ./
RUN true # This is a workaround to https://github.com/moby/moby/issues/37965
COPY --from=builder application/ ./
RUN true # This is a workaround to https://github.com/moby/moby/issues/37965
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]