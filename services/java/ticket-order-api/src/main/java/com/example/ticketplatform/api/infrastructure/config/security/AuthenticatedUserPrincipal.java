package com.example.ticketplatform.api.infrastructure.config.security;

import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.util.UUID;

public record AuthenticatedUserPrincipal(UUID userId, String email, UserRole role) {

  @Override
  public String toString() {
    return userId.toString();
  }
}
