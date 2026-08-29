package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserQueryPersistenceAdapter implements UserQueryRepositoryPort {

  private final UserJpaRepository primaryUserRepository;
  private final ReadReplicaUserJpaRepository readReplicaUserRepository;
  private final UserMapper userMapper;
  private final ReadQueryExecutor readQueryExecutor;

  @Override
  public Optional<User> findById(UUID id) {
    return readQueryExecutor.execute(
        () -> readReplicaUserRepository.findById(id).map(userMapper::toDomain),
        () -> primaryUserRepository.findById(id).map(userMapper::toDomain));
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return readQueryExecutor.execute(
        () -> readReplicaUserRepository.findByEmail(email).map(userMapper::toDomain),
        () -> primaryUserRepository.findByEmail(email).map(userMapper::toDomain));
  }
}
