package com.example.ticketplatform.api.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.example.ticketplatform.api.domain.model.user.User;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
