package com.example.ticketplatform.api.infrastructure.config.security;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@ManagementContextConfiguration(ManagementContextType.CHILD)
class ManagementSecurityConfig {

  // Access control for this port is network-level (not published/routed publicly), not app-level.
  @Bean
  @Order(0)
  SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/actuator/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .build();
  }
}
