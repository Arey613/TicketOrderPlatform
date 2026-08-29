package com.example.ticketplatform.api.infrastructure.config.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest
class PersistenceConfigTest {

  @Autowired
  private DataSource primaryDataSource;

  @Autowired
  @Qualifier("readReplicaDataSource")
  private DataSource readReplicaDataSource;

  @Autowired
  @Qualifier("primaryTransactionManager")
  private PlatformTransactionManager primaryTransactionManager;

  @Autowired
  @Qualifier("readReplicaTransactionManager")
  private PlatformTransactionManager readReplicaTransactionManager;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private JpaQueryCatalog jpaQueryCatalog;

  @Test
  void exposesPrimaryAndReadReplicaDatasources() {
    assertThat(primaryDataSource).isNotSameAs(readReplicaDataSource);
  }

  @Test
  void exposesSeparatePrimaryAndReadReplicaTransactionManagers() {
    assertThat(primaryTransactionManager).isNotSameAs(readReplicaTransactionManager);
  }

  @Test
  void doesNotExposeAnalyticalDatasourceBean() {
    assertThat(applicationContext.containsBean("analyticalDataSource")).isFalse();
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

    assertThat(userPasswordHashColumns).isEqualTo(1);
  }

  @Test
  void exposesRequiredJpaQueries() {
    assertThat(JpaQueryCatalog.requiredQueryKeys()).allSatisfy(key -> assertThat(jpaQueryCatalog.get(key)).isNotBlank());
  }
}
