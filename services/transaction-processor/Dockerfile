FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/transaction-processor-*.jar /app/transaction-processor.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/transaction-processor.jar"]
