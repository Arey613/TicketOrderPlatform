# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
COPY contracts/openapi/ticket-order-api contracts/openapi/ticket-order-api
COPY database/java/migrations/ticket-order-db-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/pom.xml
COPY services/java/ticket-order-api/pom.xml services/java/ticket-order-api/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl services/java/ticket-order-api -am dependency:go-offline

COPY services/java/ticket-order-api/src services/java/ticket-order-api/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -pl services/java/ticket-order-api -am package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/services/java/ticket-order-api/target/ticket-order-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
