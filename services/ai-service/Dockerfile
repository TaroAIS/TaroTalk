FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/ai-service-0.1.0-SNAPSHOT.jar /app/ai-service.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/ai-service.jar"]
