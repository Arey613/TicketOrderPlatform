package com.example.ticketplatform.api.infrastructure.config.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PersistenceConfigTest {

  @Autowired
  private DataSource primaryDataSource;

  @Autowired
  @Qualifier("analyticalDataSource")
  private DataSource analyticalDataSource;

  @Test
  void exposesPrimaryAndAnalyticalDatasources() {
    assertThat(primaryDataSource).isNotSameAs(analyticalDataSource);
  }

  @Test
  void initializesOperationalTablesInTransactionalSchema() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(primaryDataSource);

    Integer userPasswordHashColumns =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_SCHEMA) = 'ticket_transactional'
              AND LOWER(TABLE_NAME) = 't_users'
              AND LOWER(COLUMN_NAME) = 'password_hash'
            """,
            Integer.class);

    Integer analyticalPasswordHashColumns =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE LOWER(TABLE_SCHEMA) = 'ticket_analytical'
              AND LOWER(TABLE_NAME) = 'users'
              AND LOWER(COLUMN_NAME) = 'password_hash'
            """,
            Integer.class);

    assertThat(userPasswordHashColumns).isEqualTo(1);
    assertThat(analyticalPasswordHashColumns).isZero();
  }
}
