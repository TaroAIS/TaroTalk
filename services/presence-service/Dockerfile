FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/presence-service-0.1.0-SNAPSHOT.jar /app/presence-service.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/presence-service.jar"]
