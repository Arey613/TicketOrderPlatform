package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.RegisterUserCommand;
import com.example.ticketplatform.api.application.port.in.UserCommandUseCase;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;

@Service
@RequiredArgsConstructor
public class RegistrationUserService implements UserCommandUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final UserApplicationMapper userApplicationMapper;
  private final Supplier<Instant> currentTimeSupplier;

  @Override
  public User registerUser(RegisterUserCommand command) {
    Instant createdAt = currentTimeSupplier.get();
    User newUser =
        userApplicationMapper.toUser(command, randomUUID(), UserRole.CUSTOMER, TRUE, createdAt);
    return userRepositoryPort.save(newUser);
  }
}
