package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
class UserQueryPersistenceAdapter implements UserQueryRepositoryPort {

  private final UserJpaRepository primaryUserRepository;
  private final ReadReplicaUserJpaRepository readReplicaUserRepository;
  private final EntityManagerFactory primaryEntityManagerFactory;
  private final EntityManagerFactory readReplicaEntityManagerFactory;
  private final UserMapper userMapper;
  private final UserPageMapper userPageMapper;
  private final ReadQueryExecutor readQueryExecutor;

  UserQueryPersistenceAdapter(
      UserJpaRepository primaryUserRepository,
      ReadReplicaUserJpaRepository readReplicaUserRepository,
      @Qualifier("primaryEntityManagerFactory") EntityManagerFactory primaryEntityManagerFactory,
      @Qualifier("readReplicaEntityManagerFactory") EntityManagerFactory readReplicaEntityManagerFactory,
      UserMapper userMapper,
      UserPageMapper userPageMapper,
      ReadQueryExecutor readQueryExecutor) {
    this.primaryUserRepository = primaryUserRepository;
    this.readReplicaUserRepository = readReplicaUserRepository;
    this.primaryEntityManagerFactory = primaryEntityManagerFactory;
    this.readReplicaEntityManagerFactory = readReplicaEntityManagerFactory;
    this.userMapper = userMapper;
    this.userPageMapper = userPageMapper;
    this.readQueryExecutor = readQueryExecutor;
  }

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

  @Override
  public PageResult<User> findUsers(
      UserRole role,
      Boolean enabled,
      String search,
      PageRequest pageRequest) {
    UserRoleEntity entityRole = userMapper.toEntityRole(role);
    return readQueryExecutor.execute(
        () -> findUsers(readReplicaEntityManagerFactory, entityRole, enabled, search, pageRequest),
        () -> findUsers(primaryEntityManagerFactory, entityRole, enabled, search, pageRequest));
  }

  private PageResult<User> findUsers(
      EntityManagerFactory entityManagerFactory,
      UserRoleEntity role,
      Boolean enabled,
      String search,
      PageRequest pageRequest) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    try {
      TypedQuery<UserEntity> query =
          entityManager.createNamedQuery("UserEntity.findUsers", UserEntity.class);
      applyFilters(query, role, enabled, search);
      query.setParameter("sort", pageRequest.sort());
      query.setFirstResult(pageRequest.page() * pageRequest.size());
      query.setMaxResults(pageRequest.size());

      TypedQuery<Long> countQuery = entityManager.createNamedQuery("UserEntity.countUsers", Long.class);
      applyFilters(countQuery, role, enabled, search);
      return userPageMapper.toPageResult(query.getResultList(), countQuery.getSingleResult(), pageRequest);
    } finally {
      entityManager.close();
    }
  }

  private void applyFilters(
      TypedQuery<?> query,
      UserRoleEntity role,
      Boolean enabled,
      String search) {
    query.setParameter("role", role);
    query.setParameter("enabled", enabled);
    query.setParameter("search", search);
  }
}
