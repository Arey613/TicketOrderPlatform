package com.example.ticketplatform.api.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class UserQueryPersistenceAdapterTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");
  private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

  @Autowired
  private PlatformTransactionManager primaryTransactionManager;

  @Autowired
  private DataSource primaryDataSource;

  @Autowired
  private UserPersistenceAdapter commandAdapter;

  @Autowired
  private UserQueryPersistenceAdapter queryAdapter;

  @BeforeEach
  void setUp() {
    new JdbcTemplate(primaryDataSource).update("DELETE FROM ticket_transactional.t_users");
    save(
        new User(
            USER_ID,
            "user-query@example.com",
            "{noop}secret",
            UserRole.CUSTOMER,
            true,
            NOW,
            NOW));
  }

  @Test
  void readsUserByEmailByNamedQuery() {
    assertThat(queryAdapter.findByEmail("user-query@example.com")).map(User::id).contains(USER_ID);
  }

  private void save(User user) {
    new TransactionTemplate(primaryTransactionManager).executeWithoutResult(status -> commandAdapter.save(user));
  }
}
