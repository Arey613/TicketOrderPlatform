package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.infrastructure.config.persistence.JpaQueryCatalog;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UserQueryPersistenceAdapter implements UserQueryRepositoryPort {

  @PersistenceContext(unitName = "readReplica")
  private EntityManager readReplicaEntityManager;

  @PersistenceContext(unitName = "primary")
  private EntityManager primaryEntityManager;

  private final UserJpaRepository primaryUserRepository;
  private final UserMapper userMapper;
  private final JpaQueryCatalog jpaQueryCatalog;
  private final ReadQueryExecutor readQueryExecutor;

  @Override
  public Optional<User> findById(UUID id) {
    return readQueryExecutor.execute(
        () -> Optional.ofNullable(readReplicaEntityManager.find(UserEntity.class, id)).map(userMapper::toDomain),
        () -> primaryUserRepository.findById(id).map(userMapper::toDomain));
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEntityManager
                .createQuery(
                    jpaQueryCatalog.get(UserQueryPersistenceAdapter.class, "findByEmail"),
                    UserEntity.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .map(userMapper::toDomain),
        () ->
            primaryEntityManager
                .createQuery(
                    jpaQueryCatalog.get(UserQueryPersistenceAdapter.class, "findByEmail"),
                    UserEntity.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst()
                .map(userMapper::toDomain));
  }
}
