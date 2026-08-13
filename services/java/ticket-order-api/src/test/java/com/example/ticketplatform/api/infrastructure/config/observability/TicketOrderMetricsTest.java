package com.example.ticketplatform.api.infrastructure.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class TicketOrderMetricsTest {

  @Test
  void recordsAuthMetrics() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    TicketOrderMetrics metrics =
        new TicketOrderMetrics(meterRegistry, new ObservabilityProperties(null, null, null));

    metrics.recordLoginAttempt();
    metrics.recordLoginSuccess();
    metrics.recordLoginFailure("invalid_credentials");

    assertThat(meterRegistry.counter("ticket.auth.login.attempts").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("ticket.auth.login.success", "result", "success").count())
        .isEqualTo(1.0);
    assertThat(
            meterRegistry
                .counter(
                    "ticket.auth.login.failure",
                    "result",
                    "failure",
                    "reason",
                    "invalid_credentials")
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void skipsAuthMetricsWhenDisabled() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ObservabilityProperties properties =
        new ObservabilityProperties(
            null, null, new ObservabilityProperties.Metrics(true, false));
    TicketOrderMetrics metrics = new TicketOrderMetrics(meterRegistry, properties);

    metrics.recordLoginAttempt();

    assertThat(meterRegistry.find("ticket.auth.login.attempts").counter()).isNull();
  }
}
