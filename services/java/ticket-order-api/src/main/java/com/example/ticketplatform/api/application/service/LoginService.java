package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.LoginUseCase;
import com.example.ticketplatform.api.application.port.out.PasswordMatcherPort;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class LoginService implements LoginUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final PasswordMatcherPort passwordMatcherPort;

  @Override
  public User login(String login, String password) {
    User user =
        userRepositoryPort
            .findByEmail(login)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (!user.enabled() || !passwordMatcherPort.matches(password, user.passwordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    return user;
  }
}
