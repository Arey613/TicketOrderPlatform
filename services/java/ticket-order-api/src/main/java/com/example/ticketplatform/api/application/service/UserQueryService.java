package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.UserQueryUseCase;
import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserQueryService implements UserQueryUseCase {

  private final UserQueryRepositoryPort userQueryRepositoryPort;

  @Override
  public User getUser(UUID userId) {
    return userQueryRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }

  @Override
  public User getUserByEmail(String email) {
    return userQueryRepositoryPort
        .findByEmail(email)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + email));
  }
}
