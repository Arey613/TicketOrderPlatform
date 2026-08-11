package com.example.ticketplatform.api.adapter.in.security;

import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static java.lang.Boolean.TRUE;

@Service
@RequiredArgsConstructor
class DomainUserDetailsService implements UserDetailsService {

  private final UserRepositoryPort userRepositoryPort;

  @Override
  public UserDetails loadUserByUsername(String username) {
    return userRepositoryPort
        .findByEmail(username)
        .map(this::toUserDetails)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  private UserDetails toUserDetails(User user) {
    return org.springframework.security.core.userdetails.User.withUsername(user.email())
        .password(user.passwordHash())
        .roles(user.role().name())
        .disabled(!TRUE.equals(user.enabled()))
        .build();
  }
}
