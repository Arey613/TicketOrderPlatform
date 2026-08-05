package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.RegisterUserCommand;
import com.example.ticketplatform.api.application.port.in.UserCommandUseCase;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationUserService implements UserCommandUseCase {

  private final UserRepositoryPort userRepositoryPort;

  @Override
  public User registerUser(RegisterUserCommand command) {
    Instant createdAt = Instant.now();
    User newUser =
        new User(
            UUID.randomUUID(),
            command.userEmail(),
            command.passwordHash(),
            UserRole.CUSTOMER,
            true,
            createdAt,
            createdAt);
    return userRepositoryPort.save(newUser);
  }
}
