# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-25

ARG MIGRATION_MODULE_PATH
ARG MIGRATION_PROFILE
ARG FLYWAY_VERSION=13.0.0
ARG POSTGRESQL_VERSION=42.7.7
ENV MIGRATION_PROFILE=${MIGRATION_PROFILE}
ENV MAVEN_REPO_LOCAL=/workspace/.m2/repository

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY database/java/migrations/ticket-order-db-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/src database/java/migrations/ticket-order-db-migrations/ticket-order-transactional-migrations/src
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/pom.xml database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/pom.xml
COPY database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/src database/java/migrations/ticket-order-db-migrations/ticket-order-analytical-migrations/src

WORKDIR /workspace/${MIGRATION_MODULE_PATH}

RUN mvn -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -P${MIGRATION_PROFILE} process-resources dependency:go-offline
RUN mvn -Dmaven.repo.local=${MAVEN_REPO_LOCAL} dependency:get -Dartifact=org.flywaydb:flyway-database-postgresql:${FLYWAY_VERSION}
RUN mvn -Dmaven.repo.local=${MAVEN_REPO_LOCAL} dependency:get -Dartifact=org.postgresql:postgresql:${POSTGRESQL_VERSION}

CMD ["sh", "-c", "mvn -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -P${MIGRATION_PROFILE} process-resources flyway:migrate"]
