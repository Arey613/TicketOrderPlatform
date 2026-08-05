package com.example.ticketplatform.api.adapter.out.security;

import com.example.ticketplatform.api.application.port.out.PasswordHasherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringSecurityPasswordHasher implements PasswordHasherPort {

  private final PasswordEncoder passwordEncoder;

  @Override
  public String hash(String rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }
}
