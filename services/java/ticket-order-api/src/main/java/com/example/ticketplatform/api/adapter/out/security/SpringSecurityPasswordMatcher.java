package com.example.ticketplatform.api.adapter.out.security;

import com.example.ticketplatform.api.application.port.out.PasswordMatcherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringSecurityPasswordMatcher implements PasswordMatcherPort {

  private final PasswordEncoder passwordEncoder;

  @Override
  public boolean matches(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
  }
}
