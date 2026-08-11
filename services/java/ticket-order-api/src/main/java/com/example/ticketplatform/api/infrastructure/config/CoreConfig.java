package com.example.ticketplatform.api.infrastructure.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CoreConfig {

  @Bean
  Clock clock(@Value("${application.time-zone:UTC}") String timeZone) {
    return Clock.system(ZoneId.of(timeZone));
  }

  @Bean
  Supplier<Instant> currentTimeSupplier(Clock clock) {
    return clock::instant;
  }
}
