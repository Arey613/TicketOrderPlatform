package com.example.ticketplatform.api.infrastructure.config.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.infrastructure.config.observability.ObservabilityProperties;
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
            null, null, new ObservabilityProperties.Metrics(true, false, true));
    TicketOrderMetrics metrics = new TicketOrderMetrics(meterRegistry, properties);

    metrics.recordLoginAttempt();

    assertThat(meterRegistry.find("ticket.auth.login.attempts").counter()).isNull();
  }

  @Test
  void recordsEventMediaMetrics() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    TicketOrderMetrics metrics =
        new TicketOrderMetrics(meterRegistry, new ObservabilityProperties(null, null, null));

    metrics.recordEventImageAttachAccepted();
    metrics.recordEventImageAttachRejected("IllegalArgumentException");
    metrics.recordEventVideoUploadUrlIssued();
    metrics.recordEventVideoUploadUrlRejected("SecurityException");

    assertThat(
            meterRegistry
                .counter("ticket.event.media.image.attach", "result", "accepted")
                .count())
        .isEqualTo(1.0);
    assertThat(
            meterRegistry
                .counter(
                    "ticket.event.media.image.attach",
                    "result",
                    "rejected",
                    "reason",
                    "IllegalArgumentException")
                .count())
        .isEqualTo(1.0);
    assertThat(
            meterRegistry
                .counter("ticket.event.media.video.upload_url", "result", "issued")
                .count())
        .isEqualTo(1.0);
    assertThat(
            meterRegistry
                .counter(
                    "ticket.event.media.video.upload_url",
                    "result",
                    "rejected",
                    "reason",
                    "SecurityException")
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void skipsEventMediaMetricsWhenDisabled() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    ObservabilityProperties properties =
        new ObservabilityProperties(
            null, null, new ObservabilityProperties.Metrics(true, true, false));
    TicketOrderMetrics metrics = new TicketOrderMetrics(meterRegistry, properties);

    metrics.recordEventImageAttachAccepted();

    assertThat(meterRegistry.find("ticket.event.media.image.attach").counter()).isNull();
  }
}
