package com.example.ticketplatform.api.infrastructure.config.observability.metrics;

import com.example.ticketplatform.api.infrastructure.config.observability.ObservabilityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketOrderMetrics {

  private static final String RESULT = "result";
  private static final String REASON = "reason";

  private final MeterRegistry meterRegistry;
  private final ObservabilityProperties observabilityProperties;

  public void recordLoginAttempt() {
    if (!authMetricsEnabled()) {
      return;
    }
    meterRegistry.counter("ticket.auth.login.attempts").increment();
  }

  public void recordLoginSuccess() {
    if (!authMetricsEnabled()) {
      return;
    }
    meterRegistry.counter("ticket.auth.login.success", RESULT, "success").increment();
  }

  public void recordLoginFailure(String reason) {
    if (!authMetricsEnabled()) {
      return;
    }
    meterRegistry.counter("ticket.auth.login.failure", RESULT, "failure", REASON, reason)
        .increment();
  }

  private boolean authMetricsEnabled() {
    return observabilityProperties.metrics().enabled()
        && observabilityProperties.metrics().authMetricsEnabled();
  }
}
