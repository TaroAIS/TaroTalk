FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/relationship-service-0.1.0-SNAPSHOT.jar /app/relationship-service.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/relationship-service.jar"]
