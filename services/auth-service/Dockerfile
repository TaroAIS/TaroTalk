FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/auth-service-0.1.0-SNAPSHOT.jar /app/auth-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
