package com.example.ticketplatform.api.adapter.out.persistence.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);

  @Query(
      """
      select u
      from UserEntity u
      where (:role is null or u.role = :role)
        and (:enabled is null or u.enabled = :enabled)
        and (:search is null or lower(u.email) like lower(concat('%', :search, '%')))
      """)
  Page<UserEntity> findUsers(UserRoleEntity role, Boolean enabled, String search, Pageable pageable);
}
