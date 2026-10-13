package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.application.port.in.PageMetadata;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.out.UserQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  @Override
  public PageResult<User> findUsers(
      UserRole role,
      Boolean enabled,
      String search,
      PageRequest pageRequest) {
    UserRoleEntity entityRole = userMapper.toEntityRole(role);
    Pageable pageable = toPageable(pageRequest);
    return readQueryExecutor.execute(
        () -> toPageResult(readReplicaUserRepository.findUsers(entityRole, enabled, search, pageable)),
        () -> toPageResult(primaryUserRepository.findUsers(entityRole, enabled, search, pageable)));
  }

  private PageResult<User> toPageResult(Page<UserEntity> page) {
    List<User> items = page.getContent().stream().map(userMapper::toDomain).toList();
    return new PageResult<>(
        items,
        new PageMetadata(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()));
  }

  private Pageable toPageable(PageRequest pageRequest) {
    String[] sortParts = pageRequest.sort().split(",", 2);
    String property = sortParts[0];
    Sort.Direction direction =
        sortParts.length == 2 ? Sort.Direction.fromString(sortParts[1]) : Sort.Direction.ASC;
    return org.springframework.data.domain.PageRequest.of(
        pageRequest.page(), pageRequest.size(), Sort.by(direction, property).and(Sort.by("id")));
  }
}
