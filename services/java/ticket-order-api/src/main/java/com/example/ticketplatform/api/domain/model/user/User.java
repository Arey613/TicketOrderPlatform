package com.example.ticketplatform.api.domain.model.user;

import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record User(
    UUID id,
    String email,
    String passwordHash,
    UserRole role,
    Boolean enabled,
    Instant createdAt,
    Instant updatedAt) {

  public User {
    requireNonNull(id, "id must not be null");
    requireNonNull(email, "email must not be null");
    requireNonNull(passwordHash, "passwordHash must not be null");
    requireNonNull(role, "role must not be null");
    requireNonNull(enabled, "enabled must not be null");
    requireNonNull(createdAt, "createdAt must not be null");
    requireNonNull(updatedAt, "updatedAt must not be null");
  }
}
