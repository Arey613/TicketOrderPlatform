package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.domain.model.user.User;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface UserMapper {

    UserJpaEntity toEntity(User user);

    User toDomain(UserJpaEntity entity);
}
