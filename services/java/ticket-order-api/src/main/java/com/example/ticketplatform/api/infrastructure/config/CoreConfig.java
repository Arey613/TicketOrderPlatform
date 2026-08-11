package com.example.ticketplatform.api.infrastructure.config;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CoreConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  Supplier<Instant> currentTimeSupplier(Clock clock) {
    return clock::instant;
  }
}
