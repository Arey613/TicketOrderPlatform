package com.example.ticketplatform.api.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

  @Test
  void createsCorsConfigurationFromProperties() {
    CorsProperties properties = new CorsProperties();
    properties.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
    properties.setAllowedMethods(java.util.List.of("GET", "OPTIONS"));
    properties.setAllowedHeaders(java.util.List.of("Content-Type"));
    properties.setAllowCredentials(true);
    properties.setMaxAge(1800);

    CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(properties);

    CorsConfiguration configuration =
        source.getCorsConfiguration(new MockHttpServletRequest("POST", "/auth/login"));
    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
    assertThat(configuration.getAllowedMethods()).containsExactly("GET", "OPTIONS");
    assertThat(configuration.getAllowedHeaders()).containsExactly("Content-Type");
    assertThat(configuration.getAllowCredentials()).isTrue();
    assertThat(configuration.getMaxAge()).isEqualTo(1800);
  }
}
