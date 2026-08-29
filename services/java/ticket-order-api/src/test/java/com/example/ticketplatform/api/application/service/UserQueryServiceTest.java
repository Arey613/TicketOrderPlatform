package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserQueryServiceTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final Instant TEST_TIME = Instant.parse("2026-08-05T00:00:00Z");
  private static final User USER =
      new User(
          USER_ID,
          "customer@example.com",
          "{noop}secret",
          UserRole.CUSTOMER,
          true,
          TEST_TIME,
          TEST_TIME);

  @Test
  void returnsUserById() {
    UserQueryService service = new UserQueryService(TestUserRepositoryPort.withUser(USER));

    User result = service.getUser(USER_ID);

    assertThat(result).isEqualTo(USER);
  }

  @Test
  void rejectsMissingUser() {
    UserQueryService service = new UserQueryService(TestUserRepositoryPort.empty());

    assertThatThrownBy(() -> service.getUser(USER_ID)).isInstanceOf(NoSuchElementException.class);
  }

  private static class TestUserRepositoryPort implements UserQueryRepositoryPort {

    private final Map<UUID, User> usersById = new HashMap<>();

    private TestUserRepositoryPort(List<User> users) {
      for (User user : users) {
        usersById.put(user.id(), user);
      }
    }

    static TestUserRepositoryPort empty() {
      return new TestUserRepositoryPort(List.of());
    }

    static TestUserRepositoryPort withUser(User user) {
      return new TestUserRepositoryPort(List.of(user));
    }

    @Override
    public Optional<User> findById(UUID id) {
      return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
      return usersById.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }

  }
}
