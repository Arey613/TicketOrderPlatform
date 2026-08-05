package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.user.User;
import java.util.UUID;

public interface GetUserUseCase {

  User getUser(UUID userId);
}
