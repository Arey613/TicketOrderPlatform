package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface UserMapper {

  @Mapping(target = "role", source = "role", qualifiedByName = "toEntityRole")
  UserEntity toEntity(User user);

  @Mapping(target = "role", source = "role", qualifiedByName = "toDomainRole")
  User toDomain(UserEntity entity);

  @Named("toEntityRole")
  default UserRoleEntity toEntityRole(UserRole role) {
    return role == null ? null : UserRoleEntity.valueOf(role.name());
  }

  @Named("toDomainRole")
  default UserRole toDomainRole(UserRoleEntity role) {
    return role == null ? null : UserRole.valueOf(role.name());
  }
}
