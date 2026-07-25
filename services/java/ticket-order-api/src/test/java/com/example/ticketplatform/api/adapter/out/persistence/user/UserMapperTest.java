package com.example.ticketplatform.api.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {

    @Test
    void mapsBetweenDomainAndJpaEntity() {
        Instant createdAt = Instant.parse("2026-07-24T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-07-24T11:00:00Z");
        User domainUser = new User(
                UUID.fromString("019f9344-d6d0-74c0-964d-982dcb0cd316"),
                "user@example.com",
                "{noop}secret",
                UserRole.CUSTOMER,
                true,
                createdAt,
                updatedAt);

        UserMapper mapper = Mappers.getMapper(UserMapper.class);
        UserJpaEntity entity = mapper.toEntity(domainUser);
        User mappedUser = mapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(domainUser.id());
        assertThat(entity.getEmail()).isEqualTo(domainUser.email());
        assertThat(entity.getPasswordHash()).isEqualTo(domainUser.passwordHash());
        assertThat(entity.getRole()).isEqualTo(domainUser.role());
        assertThat(entity.isEnabled()).isEqualTo(domainUser.enabled());
        assertThat(entity.getCreatedAt()).isEqualTo(domainUser.createdAt());
        assertThat(entity.getUpdatedAt()).isEqualTo(domainUser.updatedAt());
        assertThat(mappedUser).isEqualTo(domainUser);
    }
}
