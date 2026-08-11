package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.UserQueryUseCase;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserQueryService implements UserQueryUseCase {

  private final UserRepositoryPort userRepositoryPort;

  @Override
  public User getUser(UUID userId) {
    return userRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }

  @Override
  public User getUserByEmail(String email) {
    return userRepositoryPort
        .findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + email));
  }
}
