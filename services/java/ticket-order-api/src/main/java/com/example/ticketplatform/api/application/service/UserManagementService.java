package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.in.UpdateUserCommand;
import com.example.ticketplatform.api.application.port.in.UserListQuery;
import com.example.ticketplatform.api.application.port.in.UserManagementUseCase;
import com.example.ticketplatform.api.application.port.out.UserCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.application.port.out.UserSessionRevocationPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Service
@RequiredArgsConstructor
class UserManagementService implements UserManagementUseCase {

  private final UserQueryRepositoryPort userQueryRepositoryPort;
  private final UserCommandRepositoryPort userCommandRepositoryPort;
  private final UserSessionRevocationPort userSessionRevocationPort;
  private final Supplier<Instant> currentTimeSupplier;

  @Override
  @Transactional(readOnly = true)
  public PageResult<User> listUsers(UserListQuery query) {
    return userQueryRepositoryPort.findUsers(
        query.role(), query.enabled(), query.search(), query.pageRequest());
  }

  @Override
  @Transactional(readOnly = true)
  public User getManagedUser(UUID userId) {
    return userQueryRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }

  @Override
  @Transactional
  public User updateUser(UUID actorUserId, UUID targetUserId, UpdateUserCommand command) {
    User actor = getCommandUser(actorUserId);
    if (actor.role() != UserRole.ADMIN || !TRUE.equals(actor.enabled())) {
      throw new SecurityException("Actor is not an enabled admin: " + actorUserId);
    }

    User target = getCommandUser(targetUserId);
    if (target.role() == UserRole.ADMIN) {
      throw new SecurityException("Admin users are not editable: " + targetUserId);
    }
    if (command.role() == UserRole.ADMIN) {
      throw new SecurityException("Admin role assignment is not allowed: " + targetUserId);
    }

    Instant updatedAt = currentTimeSupplier.get();
    User updated =
        new User(
            target.id(),
            target.email(),
            target.passwordHash(),
            command.role(),
            command.enabled(),
            target.createdAt(),
            updatedAt);
    User saved = userCommandRepositoryPort.update(updated);
    if (TRUE.equals(target.enabled()) && FALSE.equals(command.enabled())) {
      userSessionRevocationPort.revokeSessions(target.id());
    }
    return saved;
  }

  private User getCommandUser(UUID userId) {
    return userCommandRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }
}
