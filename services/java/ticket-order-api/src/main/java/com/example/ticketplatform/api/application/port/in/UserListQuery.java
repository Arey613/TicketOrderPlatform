package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.user.UserRole;

public record UserListQuery(
    UserRole role,
    Boolean enabled,
    String search,
    PageRequest pageRequest) {}
