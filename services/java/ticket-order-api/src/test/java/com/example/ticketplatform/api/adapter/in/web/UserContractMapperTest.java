package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserContractMapperTest {

    @Test
    void mapsDomainUserToContractResponse() {
        Instant createdAt = Instant.parse("2026-07-24T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-07-24T11:00:00Z");
        User user = new User(
                UUID.fromString("019f9344-d6d0-74c0-964d-982dcb0cd316"),
                "user@example.com",
                "{noop}secret",
                UserRole.ADMIN,
                true,
                createdAt,
                updatedAt);

        UserContractMapper mapper = Mappers.getMapper(UserContractMapper.class);
        UserResponse response = mapper.toContract(user);

        assertThat(response.getId()).isEqualTo(user.id());
        assertThat(response.getEmail()).isEqualTo(user.email());
        assertThat(response.getRole().getValue()).isEqualTo(user.role().name());
        assertThat(response.getEnabled()).isEqualTo(user.enabled());
        assertThat(response.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-07-24T10:00:00Z"));
        assertThat(response.getUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-07-24T11:00:00Z"));
    }
}
