package com.example.ticketplatform.api.domain.model.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record User(
    UUID id,
    String email,
    String passwordHash,
    UserRole role,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {

  public User {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    Objects.requireNonNull(role, "role must not be null");
    Objects.requireNonNull(createdAt, "createdAt must not be null");
    Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
