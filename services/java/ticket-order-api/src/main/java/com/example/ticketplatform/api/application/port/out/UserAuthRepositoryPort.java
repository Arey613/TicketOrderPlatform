package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.domain.model.user.User;
import java.util.Optional;

public interface UserAuthRepositoryPort {

  Optional<User> findByEmail(String email);
}
