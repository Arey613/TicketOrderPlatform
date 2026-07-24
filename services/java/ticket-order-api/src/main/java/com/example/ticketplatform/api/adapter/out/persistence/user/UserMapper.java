package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.domain.model.user.User;

class UserMapper {

    UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id(),
                user.email(),
                user.passwordHash(),
                user.role(),
                user.enabled(),
                user.createdAt(),
                user.updatedAt());
    }

    User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
