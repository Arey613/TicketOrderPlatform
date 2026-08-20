package com.example.ticketplatform.api.infrastructure.config.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class MigrationSchemaBoundaryTest {

  private static final Path MIGRATIONS_ROOT =
      Path.of(
          "../../..",
          "database/java/migrations/ticket-order-db-migrations");
  private static final List<Path> MIGRATION_RESOURCE_PATHS =
      List.of(
          MIGRATIONS_ROOT.resolve(
              "ticket-order-transactional-migrations/src/main/resources/db"),
          MIGRATIONS_ROOT.resolve("ticket-order-analytical-migrations/src/main/resources/db"));

  @Test
  void migrationSqlDoesNotHardcodeSchemaQualifiedObjects() throws IOException {
    for (Path resourcePath : MIGRATION_RESOURCE_PATHS) {
      try (Stream<Path> files = Files.walk(resourcePath)) {
        Iterable<Path> sqlFiles =
            files.filter(path -> path.getFileName().toString().endsWith(".sql")).toList();

        assertThat(sqlFiles).isNotEmpty();

        for (Path sqlFile : sqlFiles) {
          String sql = Files.readString(sqlFile);

          assertThat(sql).doesNotContain("ticket_transactional.");
          assertThat(sql).doesNotContain("ticket_analytical.");
          assertThat(sql).doesNotContain("public.");
        }
      }
    }
  }

  @Test
  void userPasswordHashExistsOnlyInTransactionalUserTable() throws IOException {
    Path transactionalUsers =
        MIGRATIONS_ROOT.resolve(
            "ticket-order-transactional-migrations/src/main/resources/db/migrations/V1.0001__create_users.sql");
    Path analyticalUsers =
        MIGRATIONS_ROOT.resolve(
            "ticket-order-analytical-migrations/src/main/resources/db/migrations/V1.0001__create_read_model_tables.sql");

    assertThat(Files.readString(transactionalUsers)).contains("password_hash");
    assertThat(Files.readString(analyticalUsers)).doesNotContain("password_hash");
  }
}
