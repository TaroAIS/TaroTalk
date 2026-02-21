FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/event-service-0.1.0-SNAPSHOT.jar /app/event-service.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "/app/event-service.jar"]
