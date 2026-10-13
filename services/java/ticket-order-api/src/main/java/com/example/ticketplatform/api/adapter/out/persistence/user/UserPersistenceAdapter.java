package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.application.port.out.UserAuthRepositoryPort;
import com.example.ticketplatform.api.application.port.out.UserCommandRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserCommandRepositoryPort, UserAuthRepositoryPort {

  private final UserJpaRepository repository;
  private final UserMapper mapper;

  @Override
  public User save(User user) {
    return mapper.toDomain(repository.save(mapper.toEntity(user)));
  }

  @Override
  public User update(User user) {
    UserEntity entity =
        repository
            .findById(user.id())
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + user.id()));
    entity.setRole(mapper.toEntityRole(user.role()));
    entity.setEnabled(user.enabled());
    entity.setUpdatedAt(user.updatedAt());
    return mapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<User> findById(UUID id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return repository.findByEmail(email).map(mapper::toDomain);
  }

}
