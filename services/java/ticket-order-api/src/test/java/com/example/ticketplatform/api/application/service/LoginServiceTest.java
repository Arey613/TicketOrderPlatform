package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.application.port.out.PasswordMatcherPort;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class LoginServiceTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
  private static final Instant TEST_TIME = Instant.parse("2026-08-05T00:00:00Z");
  private static final User ENABLED_USER = user("customer@example.com", true);
  private static final User DISABLED_USER = user("disabled@example.com", false);

  @Test
  void returnsEnabledUserWhenPasswordMatches() {
    TestUserRepositoryPort users = TestUserRepositoryPort.withUser(ENABLED_USER);
    TestPasswordMatcherPort passwords = new TestPasswordMatcherPort(true);
    LoginService service = new LoginService(users, passwords);

    User result = service.login("customer@example.com", "secret");

    assertThat(result).isEqualTo(ENABLED_USER);
    assertThat(passwords.lastRawPassword).isEqualTo("secret");
    assertThat(passwords.lastEncodedPassword).isEqualTo("{noop}secret");
  }

  @Test
  void rejectsMissingEmail() {
    TestPasswordMatcherPort passwords = new TestPasswordMatcherPort(true);
    LoginService service = new LoginService(TestUserRepositoryPort.empty(), passwords);

    assertThatThrownBy(() -> service.login("missing@example.com", "secret"))
        .isInstanceOf(BadCredentialsException.class);
    assertThat(passwords.wasCalled).isFalse();
  }

  @Test
  void rejectsWrongPassword() {
    LoginService service =
        new LoginService(TestUserRepositoryPort.withUser(ENABLED_USER), new TestPasswordMatcherPort(false));

    assertThatThrownBy(() -> service.login("customer@example.com", "wrong"))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void rejectsDisabledUser() {
    TestPasswordMatcherPort passwords = new TestPasswordMatcherPort(true);
    LoginService service = new LoginService(TestUserRepositoryPort.withUser(DISABLED_USER), passwords);

    assertThatThrownBy(() -> service.login("disabled@example.com", "secret"))
        .isInstanceOf(BadCredentialsException.class);
    assertThat(passwords.wasCalled).isFalse();
  }

  private static User user(String email, boolean enabled) {
    return new User(
        USER_ID, email, "{noop}secret", UserRole.CUSTOMER, enabled, TEST_TIME, TEST_TIME);
  }

  private static class TestUserRepositoryPort implements UserRepositoryPort {

    private final Map<String, User> usersByEmail = new HashMap<>();

    private TestUserRepositoryPort(List<User> users) {
      for (User user : users) {
        usersByEmail.put(user.email(), user);
      }
    }

    static TestUserRepositoryPort empty() {
      return new TestUserRepositoryPort(List.of());
    }

    static TestUserRepositoryPort withUser(User user) {
      return new TestUserRepositoryPort(List.of(user));
    }

    @Override
    public User save(User user) {
      usersByEmail.put(user.email(), user);
      return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
      return usersByEmail.values().stream().filter(user -> user.id().equals(id)).findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
      return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public boolean existsByEmail(String email) {
      return usersByEmail.containsKey(email);
    }
  }

  private static class TestPasswordMatcherPort implements PasswordMatcherPort {

    private final boolean matches;
    private boolean wasCalled;
    private String lastRawPassword;
    private String lastEncodedPassword;

    TestPasswordMatcherPort(boolean matches) {
      this.matches = matches;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
      wasCalled = true;
      lastRawPassword = rawPassword;
      lastEncodedPassword = encodedPassword;
      return matches;
    }
  }
}
