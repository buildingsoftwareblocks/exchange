FROM eclipse-temurin:17-jre-jammy

ADD javamelody-collector-server-1.95.0.war /

CMD java -server -Xmx128m -Djavamelody.resolution-seconds=${APM_RESOLUTION:-120} -jar /javamelody-collector-server-1.95.0.war --httpPort=${APM_SERVICE_PORT:-8080} ${APM_PARAMS:-}