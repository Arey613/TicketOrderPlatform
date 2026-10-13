package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.in.UpdateUserCommand;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.model.PageMetadata;
import com.example.ticketplatform.api.generated.contract.model.UpdateUserRequest;
import com.example.ticketplatform.api.generated.contract.model.UserListResponse;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserManagementContractMapper {

  private final UserContractMapper userContractMapper;

  UserListResponse toListResponse(PageResult<User> page) {
    return new UserListResponse(toContractList(page.items()), toContractPage(page.page()));
  }

  UpdateUserCommand toCommand(UpdateUserRequest request) {
    return new UpdateUserCommand(
        userContractMapper.toDomainRole(request.getRole()), request.getEnabled());
  }

  private List<UserResponse> toContractList(List<User> users) {
    return users.stream().map(userContractMapper::toContract).toList();
  }

  private PageMetadata toContractPage(
      com.example.ticketplatform.api.application.port.in.PageMetadata page) {
    return new PageMetadata(
        page.number(), page.size(), page.totalElements(), page.totalPages(), page.first(), page.last());
  }
}
