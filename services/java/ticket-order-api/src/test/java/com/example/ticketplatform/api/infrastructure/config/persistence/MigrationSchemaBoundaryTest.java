package com.example.ticketplatform.api.infrastructure.config.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
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
  private static final Path TRANSACTIONAL_MIGRATIONS =
      MIGRATIONS_ROOT.resolve(
          "ticket-order-transactional-migrations/src/main/resources/db/migrations");
  private static final Pattern CREATE_TABLE_PATTERN =
      Pattern.compile(
          "CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+(\\w+)\\s*\\((.*?)\\);",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final List<String> TRANSACTIONAL_ENTITY_CLASSES =
      List.of(
          "com.example.ticketplatform.api.adapter.out.persistence.user.UserEntity",
          "com.example.ticketplatform.api.adapter.out.persistence.event.EventEntity",
          "com.example.ticketplatform.api.adapter.out.persistence.event.EventDetailsEntity",
          "com.example.ticketplatform.api.adapter.out.persistence.event.EventOrderEntity");

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
            "ticket-order-analytical-migrations/src/main/resources/db/migrations/V1.0001__create_users.sql");

    assertThat(Files.readString(transactionalUsers)).contains("password_hash");
    assertThat(Files.readString(analyticalUsers)).doesNotContain("password_hash");
  }

  @Test
  void transactionalMigrationColumnsMatchHibernateMappings() throws Exception {
    Map<String, Set<String>> migrationColumns = transactionalMigrationColumns();

    for (String className : TRANSACTIONAL_ENTITY_CLASSES) {
      Class<?> entityClass = Class.forName(className);
      Table table = entityClass.getAnnotation(Table.class);

      assertThat(table).as("%s must declare @Table", className).isNotNull();
      assertThat(migrationColumns)
          .as("%s table must exist in transactional migrations", table.name())
          .containsKey(table.name());
      assertThat(migrationColumns.get(table.name()))
          .as("%s columns must match entity mapping", table.name())
          .containsExactlyInAnyOrderElementsOf(mappedColumns(entityClass));
    }
  }

  private static Map<String, Set<String>> transactionalMigrationColumns() throws IOException {
    String sql = readSqlFiles(TRANSACTIONAL_MIGRATIONS);
    Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
    Map<String, Set<String>> tables = new java.util.LinkedHashMap<>();

    while (matcher.find()) {
      String tableName = matcher.group(1);
      Set<String> columns =
          Arrays.stream(matcher.group(2).split(",\\R"))
              .map(String::strip)
              .filter(line -> !line.isBlank())
              .filter(line -> !line.toUpperCase().startsWith("CONSTRAINT "))
              .map(line -> line.split("\\s+")[0])
              .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

      tables.put(tableName, columns);
    }

    return tables;
  }

  private static String readSqlFiles(Path directory) throws IOException {
    try (Stream<Path> files = Files.walk(directory)) {
      return files
          .filter(path -> path.getFileName().toString().endsWith(".sql"))
          .sorted()
          .map(MigrationSchemaBoundaryTest::readString)
          .collect(Collectors.joining(System.lineSeparator()));
    }
  }

  private static String readString(Path path) {
    try {
      return Files.readString(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read " + path, exception);
    }
  }

  private static Set<String> mappedColumns(Class<?> entityClass) {
    return Arrays.stream(entityClass.getDeclaredFields())
        .flatMap(MigrationSchemaBoundaryTest::mappedColumnName)
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
  }

  private static Stream<String> mappedColumnName(Field field) {
    Column column = field.getAnnotation(Column.class);
    if (column != null) {
      return Stream.of(column.name());
    }

    JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
    if (joinColumn != null) {
      return Stream.of(joinColumn.name());
    }

    return Stream.empty();
  }
}
