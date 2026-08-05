package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.GetUserUseCase;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class UserQueryService implements GetUserUseCase {

  private final UserRepositoryPort userRepositoryPort;

  UserQueryService(UserRepositoryPort userRepositoryPort) {
    this.userRepositoryPort = userRepositoryPort;
  }

  @Override
  public User getUser(UUID userId) {
    return userRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }
}
