FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/scheduler-service-0.1.0-SNAPSHOT.jar /app/scheduler-service.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "/app/scheduler-service.jar"]
