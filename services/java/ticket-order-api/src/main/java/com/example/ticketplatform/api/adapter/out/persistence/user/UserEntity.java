package com.example.ticketplatform.api.adapter.out.persistence.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t_users", schema = "ticket_transactional")
@NamedQueries({
@NamedQuery(
    name = "UserEntity.findUsers",
    query =
        """
        select u
        from UserEntity u
        where (:role is null or u.role = :role)
          and (:enabled is null or u.enabled = :enabled)
          and (:search is null or lower(u.email) like lower(concat('%', :search, '%')))
        order by
          case when :sort = 'createdAt,asc' then u.createdAt end asc,
          case when :sort = 'createdAt,desc' then u.createdAt end desc,
          case when :sort = 'updatedAt,asc' then u.updatedAt end asc,
          case when :sort = 'updatedAt,desc' then u.updatedAt end desc,
          case when :sort = 'email,asc' then u.email end asc,
          case when :sort = 'email,desc' then u.email end desc,
          case when :sort = 'role,asc' then u.role end asc,
          case when :sort = 'role,desc' then u.role end desc,
          case when :sort = 'enabled,asc' then u.enabled end asc,
          case when :sort = 'enabled,desc' then u.enabled end desc,
          u.id asc
        """),
@NamedQuery(
    name = "UserEntity.countUsers",
    query =
        """
        select count(u)
        from UserEntity u
        where (:role is null or u.role = :role)
          and (:enabled is null or u.enabled = :enabled)
          and (:search is null or lower(u.email) like lower(concat('%', :search, '%')))
        """)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class UserEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true, length = 320)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 32)
  private UserRoleEntity role;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

}
