package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.UserListQuery;
import com.example.ticketplatform.api.application.port.in.UserManagementUseCase;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.api.AdminApi;
import com.example.ticketplatform.api.generated.contract.model.UpdateUserRequest;
import com.example.ticketplatform.api.generated.contract.model.UserListResponse;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;
import com.example.ticketplatform.api.generated.contract.model.UserRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AdminController implements AdminApi {

  private final UserManagementUseCase userManagementUseCase;
  private final CurrentUserProvider currentUserProvider;
  private final PaginationRequestFactory paginationRequestFactory;
  private final UserContractMapper userContractMapper;
  private final UserManagementContractMapper userManagementContractMapper;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> getManagedUser(UUID userId) {
    return ResponseEntity.ok(userContractMapper.toContract(userManagementUseCase.getManagedUser(userId)));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserListResponse> listManagedUsers(
      Integer page,
      Integer size,
      UserRole role,
      Boolean enabled,
      String search,
      String sort) {
    UserListQuery query =
        new UserListQuery(
            userContractMapper.toDomainRole(role),
            enabled,
            normalizeSearch(search),
            paginationRequestFactory.userPage(page, size, sort));
    return ResponseEntity.ok(userManagementContractMapper.toListResponse(userManagementUseCase.listUsers(query)));
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> updateManagedUser(
      UUID userId,
      UpdateUserRequest updateUserRequest) {
    User actor = currentUserProvider.currentUser();
    return ResponseEntity.ok(
        userContractMapper.toContract(
            userManagementUseCase.updateUser(
                actor.id(), userId, userManagementContractMapper.toCommand(updateUserRequest))));
  }

  private String normalizeSearch(String search) {
    return StringUtils.hasText(search) ? search.trim() : null;
  }
}
