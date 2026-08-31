package com.example.ticketplatform.api.infrastructure.config.security;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@ManagementContextConfiguration(ManagementContextType.CHILD)
class ManagementSecurityConfig {

  // Access control for this port is network-level (not published/routed publicly), not app-level.
  // GET-only is defense in depth: only read-only actuator operations are exposed here today, so
  // no other method should ever reach this port even if exposure config changes later.
  @Bean
  @Order(0)
  SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/actuator/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            authorize -> authorize.requestMatchers(HttpMethod.GET, "/actuator/**").permitAll().anyRequest().denyAll())
        .build();
  }
}
