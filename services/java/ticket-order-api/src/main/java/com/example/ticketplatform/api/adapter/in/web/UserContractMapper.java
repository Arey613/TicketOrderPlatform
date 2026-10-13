package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.in.UpdateUserCommand;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.generated.contract.model.PageMetadata;
import com.example.ticketplatform.api.generated.contract.model.UpdateUserRequest;
import com.example.ticketplatform.api.generated.contract.model.UserListResponse;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserContractMapper {

  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  UserResponse toContract(User user);

  default UserListResponse toListResponse(PageResult<User> page) {
    return new UserListResponse(toContractList(page.items()), toContractPage(page.page()));
  }

  default UpdateUserCommand toCommand(UpdateUserRequest request) {
    return new UpdateUserCommand(toDomainRole(request.getRole()), request.getEnabled());
  }

  default OffsetDateTime map(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }

  default UserRole toDomainRole(com.example.ticketplatform.api.generated.contract.model.UserRole role) {
    return role == null ? null : UserRole.valueOf(role.name());
  }

  List<UserResponse> toContractList(List<User> users);

  default PageMetadata toContractPage(com.example.ticketplatform.api.application.port.in.PageMetadata page) {
    return new PageMetadata(
        page.number(), page.size(), page.totalElements(), page.totalPages(), page.first(), page.last());
  }
}
