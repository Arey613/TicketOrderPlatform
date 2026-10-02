package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.InvalidCredentialsException;
import com.example.ticketplatform.api.application.port.in.LoginUseCase;
import com.example.ticketplatform.api.application.port.in.LoginCommand;
import com.example.ticketplatform.api.application.port.out.PasswordMatcherPort;
import com.example.ticketplatform.api.application.port.out.UserAuthRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.lang.Boolean.TRUE;

@Service
@RequiredArgsConstructor
class LoginService implements LoginUseCase {

  private final UserAuthRepositoryPort userAuthRepositoryPort;
  private final PasswordMatcherPort passwordMatcherPort;

  @Override
  public User login(LoginCommand command) {
    User user =
        userAuthRepositoryPort
            .findByEmail(command.login())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

    if (!TRUE.equals(user.enabled())
        || !passwordMatcherPort.matches(command.rawPassword(), user.passwordHash())) {
      throw new InvalidCredentialsException("Invalid credentials");
    }

    return user;
  }
}
