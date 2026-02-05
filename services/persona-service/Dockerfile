FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/persona-service-0.1.0-SNAPSHOT.jar /app/persona-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/persona-service.jar"]
