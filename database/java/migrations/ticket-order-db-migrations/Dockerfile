FROM maven:3.9-eclipse-temurin-25

ARG MIGRATION_MODULE_PATH

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY database/java/migrations/ticket-order-db-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/src database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/src
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/src database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/src

WORKDIR /workspace/${MIGRATION_MODULE_PATH}

CMD ["mvn", "process-resources", "flyway:migrate"]
