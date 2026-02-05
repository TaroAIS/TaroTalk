FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/notification-service-0.1.0-SNAPSHOT.jar /app/notification-service.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]
