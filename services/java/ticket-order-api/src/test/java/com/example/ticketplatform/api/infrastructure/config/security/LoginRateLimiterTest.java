package com.example.ticketplatform.api.infrastructure.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

  private final AtomicReference<Instant> now =
      new AtomicReference<>(Instant.parse("2026-08-31T00:00:00Z"));
  private final LoginRateLimiter loginRateLimiter = new LoginRateLimiter(now::get);

  @Test
  void blocksEmailAndIpAfterFiveFailuresWithinWindow() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "customer@example.com");
    }

    assertThat(loginRateLimiter.isBlockedByEmail("customer@example.com")).isTrue();
    assertThat(loginRateLimiter.isBlockedByIp("203.0.113.1")).isTrue();
  }

  @Test
  void doesNotBlockBeforeFifthFailure() {
    for (int i = 0; i < 4; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "customer@example.com");
    }

    assertThat(loginRateLimiter.isBlockedByEmail("customer@example.com")).isFalse();
    assertThat(loginRateLimiter.isBlockedByIp("203.0.113.1")).isFalse();
  }

  @Test
  void successResetsFailureCount() {
    for (int i = 0; i < 4; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "customer@example.com");
    }
    loginRateLimiter.recordSuccess("203.0.113.1", "customer@example.com");

    for (int i = 0; i < 4; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "customer@example.com");
    }

    assertThat(loginRateLimiter.isBlockedByEmail("customer@example.com")).isFalse();
    assertThat(loginRateLimiter.isBlockedByIp("203.0.113.1")).isFalse();
  }

  @Test
  void blockExpiresAfterWindow() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "customer@example.com");
    }
    assertThat(loginRateLimiter.isBlockedByEmail("customer@example.com")).isTrue();

    now.set(now.get().plus(Duration.ofMinutes(16)));

    assertThat(loginRateLimiter.isBlockedByEmail("customer@example.com")).isFalse();
  }

  @Test
  void emailKeyIsCaseInsensitive() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "Customer@Example.com");
    }

    assertThat(loginRateLimiter.isBlockedByEmail("customer@example.com")).isTrue();
  }

  @Test
  void differentIpsAndEmailsAreTrackedIndependently() {
    for (int i = 0; i < 5; i++) {
      loginRateLimiter.recordFailure("203.0.113.1", "customer@example.com");
    }

    assertThat(loginRateLimiter.isBlockedByIp("203.0.113.2")).isFalse();
    assertThat(loginRateLimiter.isBlockedByEmail("other@example.com")).isFalse();
  }
}
