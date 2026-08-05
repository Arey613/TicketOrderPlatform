package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.user.User;

public interface LoginUseCase {

  User login(String login, String password);
}
