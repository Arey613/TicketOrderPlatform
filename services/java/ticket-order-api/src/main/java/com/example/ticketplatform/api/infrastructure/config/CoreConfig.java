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
  ZoneId applicationZoneId(@Value("${application.time-zone:UTC}") String timeZone) {
    return ZoneId.of(timeZone);
  }

  @Bean
  Clock clock(ZoneId applicationZoneId) {
    return Clock.system(applicationZoneId);
  }

  @Bean
  Supplier<Instant> currentTimeSupplier(Clock clock) {
    return clock::instant;
  }
}
