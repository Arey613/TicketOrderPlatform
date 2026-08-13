package com.example.ticketplatform.api.infrastructure.config.observability.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CorrelationIdTest {

  private static final Instant TEST_TIME = Instant.parse("2026-08-13T10:15:30Z");

  @Test
  void generatesUuidDateCorrelationId() {
    String correlationId = CorrelationId.generate(TEST_TIME);

    assertThat(correlationId).endsWith("-13-08-2026");
    assertThat(CorrelationId.isValid(correlationId, CorrelationId.DEFAULT_VALIDATION_PATTERN))
        .isTrue();
  }

  @Test
  void generatesCorrelationIdFromConfiguredTemplate() {
    String correlationId =
        CorrelationId.generate(TEST_TIME, "ticket-order-{random-hex:8}-{date:dd-MM-yyyy}");

    assertThat(correlationId).matches("ticket-order-[0-9a-f]{8}-13-08-2026");
  }

  @Test
  void rejectsInvalidDateFormat() {
    assertThat(
            CorrelationId.isValid(
                "018f0f5e-4e7a-7a89-b2f3-5d9d4a0b91c2-2026-08-13",
                CorrelationId.DEFAULT_VALIDATION_PATTERN))
        .isFalse();
  }

  @Test
  void rejectsInvalidDateValue() {
    assertThat(
            CorrelationId.isValid(
                "018f0f5e-4e7a-7a89-b2f3-5d9d4a0b91c2-32-08-2026",
                CorrelationId.DEFAULT_VALIDATION_PATTERN))
        .isFalse();
  }

  @Test
  void validatesConfiguredPattern() {
    assertThat(CorrelationId.isValid("ticket-order-123", "^ticket-order-\\d+$")).isTrue();
    assertThat(CorrelationId.isValid("wrong-123", "^ticket-order-\\d+$")).isFalse();
  }

  @Test
  void rejectsWhitespaceEvenWhenConfiguredPatternAllowsIt() {
    assertThat(CorrelationId.isValid("ticket order 123", "^ticket order \\d+$")).isFalse();
  }

  @Test
  void rejectsInvalidConfiguredPattern() {
    assertThat(CorrelationId.isValid("ticket-order-123", "[")).isFalse();
  }
}
