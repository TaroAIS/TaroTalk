FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/feed-service-0.1.0-SNAPSHOT.jar /app/feed-service.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/feed-service.jar"]
