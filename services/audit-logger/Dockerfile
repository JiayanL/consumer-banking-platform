FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/audit-logger-*.jar /app/audit-logger.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/audit-logger.jar"]
