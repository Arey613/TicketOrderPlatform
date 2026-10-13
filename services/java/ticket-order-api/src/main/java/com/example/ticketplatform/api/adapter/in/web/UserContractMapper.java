package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserContractMapper {

  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  UserResponse toContract(User user);

  default OffsetDateTime map(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  default UserRole toDomainRole(com.example.ticketplatform.api.generated.contract.model.UserRole role) {
    return role == null ? null : UserRole.valueOf(role.name());
  }
}
