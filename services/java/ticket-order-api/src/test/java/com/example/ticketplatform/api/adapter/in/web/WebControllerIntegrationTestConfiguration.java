package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class WebControllerIntegrationTestConfiguration {

  private static final Instant TEST_TIME = Instant.parse("2026-08-05T00:00:00Z");

  @Bean
  @Primary
  UserRepositoryPort userRepositoryPort(TestUsers testUsers) {
    return testUsers;
  }

  @Bean
  TestUsers testUsers() {
    return new TestUsers();
  }

  static User user(UUID id, String email, String passwordHash, boolean enabled) {
    return new User(id, email, passwordHash, UserRole.CUSTOMER, enabled, TEST_TIME, TEST_TIME);
  }

  static class TestUsers implements UserRepositoryPort {

    private final Map<UUID, User> usersById = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();

    @Override
    public User save(User user) {
      usersById.put(user.id(), user);
      usersByEmail.put(user.email(), user);
      return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
      return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
      return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public boolean existsByEmail(String email) {
      return usersByEmail.containsKey(email);
    }

    void reset(List<User> users) {
      usersById.clear();
      usersByEmail.clear();
      for (User user : users) {
        save(user);
      }
    }
  }
}
