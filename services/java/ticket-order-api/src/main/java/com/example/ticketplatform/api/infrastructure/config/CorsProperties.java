package com.example.ticketplatform.api.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticket-order-platform.cors")
@Getter
@Setter
public class CorsProperties {

  private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
  private List<String> allowedMethods =
      new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
  private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
  private boolean allowCredentials;
  private long maxAge = 3600;
}
