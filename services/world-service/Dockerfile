FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/world-service-0.1.0-SNAPSHOT.jar /app/world-service.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "/app/world-service.jar"]
