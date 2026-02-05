FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/chat-service-0.1.0-SNAPSHOT.jar /app/chat-service.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/chat-service.jar"]
