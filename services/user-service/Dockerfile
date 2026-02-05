FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/user-service-0.1.0-SNAPSHOT.jar /app/user-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/user-service.jar"]
