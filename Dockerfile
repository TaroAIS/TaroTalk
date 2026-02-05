FROM maven:3.9.8-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
COPY libs ./libs
COPY gateway ./gateway
COPY services ./services
RUN mvn -q -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /workspace/gateway/target/gateway-0.1.0-SNAPSHOT.jar /app/gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/gateway.jar"]
