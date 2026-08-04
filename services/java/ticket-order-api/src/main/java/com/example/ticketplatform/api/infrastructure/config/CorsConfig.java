package com.example.ticketplatform.api.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
class CorsConfig {

  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.getAllowedOrigins());
    configuration.setAllowedMethods(properties.getAllowedMethods());
    configuration.setAllowedHeaders(properties.getAllowedHeaders());
    configuration.setAllowCredentials(properties.isAllowCredentials());
    configuration.setMaxAge(properties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
