package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface UserQueryRepositoryPort {

  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String email);

  PageResult<User> findUsers(UserRole role, Boolean enabled, String search, PageRequest pageRequest);
}
