package com.example.ticketplatform.api.infrastructure.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ObservabilityResourceTest {

  @Test
  void providesSeparateLoggingConfigurationAndStructureDefinitions() {
    assertThat(new ClassPathResource("logback-spring.xml").exists()).isTrue();
    assertThat(new ClassPathResource("logback-spring.yml").exists()).isTrue();
    assertThat(new ClassPathResource("observability-logging.yml").exists()).isTrue();
    assertThat(new ClassPathResource("application-log-structure.yml").exists()).isTrue();
    assertThat(new ClassPathResource("telemetry-log-structure.yml").exists()).isTrue();
    assertThat(new ClassPathResource("security-log-structure.yml").exists()).isTrue();
  }
}
