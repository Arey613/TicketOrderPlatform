package com.example.ticketplatform.api.infrastructure.config.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

  private static final int MAX_ATTEMPTS = 5;
  private static final Duration WINDOW = Duration.ofMinutes(15);
  private static final String IP_PREFIX = "ip:";
  private static final String EMAIL_PREFIX = "email:";

  private final Supplier<Instant> currentTimeSupplier;
  private final ConcurrentHashMap<String, Window> windowsByKey = new ConcurrentHashMap<>();

  boolean isBlockedByIp(String ip) {
    return isBlocked(key(IP_PREFIX, ip));
  }

  boolean isBlockedByEmail(String email) {
    return isBlocked(key(EMAIL_PREFIX, email));
  }

  void recordFailure(String ip, String email) {
    recordFailure(key(IP_PREFIX, ip));
    recordFailure(key(EMAIL_PREFIX, email));
  }

  void recordSuccess(String ip, String email) {
    remove(key(IP_PREFIX, ip));
    remove(key(EMAIL_PREFIX, email));
  }

  public void reset() {
    windowsByKey.clear();
  }

  private boolean isBlocked(String key) {
    if (key == null) {
      return false;
    }
    Window window = windowsByKey.get(key);
    if (window == null) {
      return false;
    }
    if (isExpired(window)) {
      windowsByKey.remove(key, window);
      return false;
    }
    return window.count().get() >= MAX_ATTEMPTS;
  }

  private void recordFailure(String key) {
    if (key == null) {
      return;
    }
    Instant now = currentTimeSupplier.get();
    windowsByKey.compute(
        key,
        (ignoredKey, existing) -> {
          if (existing == null || isExpired(existing, now)) {
            return new Window(now, new AtomicInteger(1));
          }
          existing.count().incrementAndGet();
          return existing;
        });
  }

  private void remove(String key) {
    if (key != null) {
      windowsByKey.remove(key);
    }
  }

  private boolean isExpired(Window window) {
    return isExpired(window, currentTimeSupplier.get());
  }

  private boolean isExpired(Window window, Instant now) {
    return window.windowStart().plus(WINDOW).isBefore(now);
  }

  private String key(String prefix, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return prefix + value.toLowerCase(Locale.ROOT);
  }

  private record Window(Instant windowStart, AtomicInteger count) {}
}
