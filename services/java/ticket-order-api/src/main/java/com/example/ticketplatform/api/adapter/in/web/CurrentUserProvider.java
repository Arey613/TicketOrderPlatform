package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.UserQueryUseCase;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CurrentUserProvider {

  private final UserQueryUseCase userQueryUseCase;

  User currentUser() {
    return optionalCurrentUser()
        .orElseThrow(() -> new NoSuchElementException("Authenticated user was not found"));
  }

  Optional<User> optionalCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.empty();
    }
    return Optional.of(userQueryUseCase.getUserByEmail(authentication.getName()));
  }
}
