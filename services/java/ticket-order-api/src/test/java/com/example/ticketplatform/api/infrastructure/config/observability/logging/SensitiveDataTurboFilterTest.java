package com.example.ticketplatform.api.infrastructure.config.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

class SensitiveDataTurboFilterTest {

  @Test
  void deniesSensitiveLogEvent() {
    SensitiveDataTurboFilter filter = new SensitiveDataTurboFilter();

    assertThat(
            filter.decide(
                null, null, null, "received password={}", new Object[] {"secret"}, null))
        .isEqualTo(FilterReply.DENY);
  }

  @Test
  void acceptsRegularLogEvent() {
    SensitiveDataTurboFilter filter = new SensitiveDataTurboFilter();

    assertThat(
            filter.decide(
                null, null, null, "auth.login.failed reason={}", new Object[] {"invalid"}, null))
        .isEqualTo(FilterReply.NEUTRAL);
  }

  @Test
  void deniesConfiguredSensitiveField() {
    SensitiveDataTurboFilter filter = new SensitiveDataTurboFilter();
    filter.setAdditionalFields("customerToken");

    assertThat(
            filter.decide(
                null, null, null, "received customerToken={}", new Object[] {"abc"}, null))
        .isEqualTo(FilterReply.DENY);
  }

  @Test
  void acceptsStructuredTelemetryLogEvent() {
    SensitiveDataTurboFilter filter = new SensitiveDataTurboFilter();

    assertThat(
            filter.decide(
                null,
                null,
                null,
                "http.request.completed method={} path={} status={} durationMs={} trace_id={} span_id={}",
                new Object[] {
                  "GET",
                  "/actuator/health",
                  200,
                  14,
                  "4bf92f3577b34da6a3ce929d0e0e4736",
                  "00f067aa0ba902b7"
                },
                null))
        .isEqualTo(FilterReply.NEUTRAL);
  }

  @Test
  void deniesStructuredEventBeforeJsonEncodingWhenSensitiveTokenIsPresent() {
    SensitiveDataTurboFilter filter = new SensitiveDataTurboFilter();

    assertThat(
            filter.decide(
                null,
                null,
                null,
                "auth.login.failed username={} authorization={}",
                new Object[] {"user@example.com", "Bearer token"},
                null))
        .isEqualTo(FilterReply.DENY);
  }
}
