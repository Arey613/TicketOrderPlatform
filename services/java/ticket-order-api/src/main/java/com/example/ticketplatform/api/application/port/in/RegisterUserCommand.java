package com.example.ticketplatform.api.application.port.in;

public record RegisterUserCommand(String userEmail, String passwordHash) {}
