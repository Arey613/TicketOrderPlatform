package com.example.ticketplatform.api.application.port.out;

public interface PasswordMatcherPort {

  boolean matches(String rawPassword, String encodedPassword);
}
