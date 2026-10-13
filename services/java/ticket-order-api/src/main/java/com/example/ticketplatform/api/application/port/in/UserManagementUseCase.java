package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.user.User;
import java.util.UUID;

public interface UserManagementUseCase {

  PageResult<User> listUsers(UserListQuery query);

  User getManagedUser(UUID userId);

  User updateUser(UUID actorUserId, UUID targetUserId, UpdateUserCommand command);
}
