FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
COPY services/java/ticket-order-api/pom.xml services/java/ticket-order-api/pom.xml
COPY services/java/ticket-order-api/src services/java/ticket-order-api/src

RUN mvn -pl services/java/ticket-order-api -am package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/services/java/ticket-order-api/target/ticket-order-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
