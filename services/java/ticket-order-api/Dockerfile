FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app --home /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY services/java/ticket-order-api/target/ticket-order-api-0.0.1-SNAPSHOT.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
