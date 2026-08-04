package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.domain.model.user.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

  User save(User user);

  Optional<User> findById(UUID id);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
