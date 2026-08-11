package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.RegisterUserCommand;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserApplicationMapper {

  @Mapping(target = "id", source = "userId")
  @Mapping(target = "email", source = "command.userEmail")
  @Mapping(target = "passwordHash", source = "command.passwordHash")
  @Mapping(target = "role", source = "role")
  @Mapping(target = "enabled", source = "enabled")
  @Mapping(target = "createdAt", source = "now")
  @Mapping(target = "updatedAt", source = "now")
  User toUser(
      RegisterUserCommand command,
      UUID userId,
      UserRole role,
      Boolean enabled,
      Instant now);
}
