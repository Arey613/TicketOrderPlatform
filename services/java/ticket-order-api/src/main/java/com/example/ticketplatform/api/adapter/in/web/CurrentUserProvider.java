package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.UserQueryUseCase;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CurrentUserProvider {

  private final UserQueryUseCase userQueryUseCase;

  User currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NoSuchElementException("Authenticated user was not found");
    }
    return userQueryUseCase.getUserByEmail(authentication.getName());
  }
}
