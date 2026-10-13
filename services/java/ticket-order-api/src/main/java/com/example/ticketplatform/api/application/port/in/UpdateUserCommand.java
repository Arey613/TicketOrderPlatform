package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.user.UserRole;

public record UpdateUserCommand(UserRole role, Boolean enabled) {}
