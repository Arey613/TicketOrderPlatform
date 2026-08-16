package com.example.ticketplatform.api.infrastructure.config.observability;

import com.example.ticketplatform.api.infrastructure.config.observability.correlation.CorrelationId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ticket-order-platform.observability")
public record ObservabilityProperties(
    @DefaultValue Logging logging, @DefaultValue Correlation correlation, @DefaultValue Metrics metrics) {

  public ObservabilityProperties {
    if (logging == null) {
      logging = defaultLogging();
    }
    if (correlation == null) {
      correlation = defaultCorrelation();
    }
    if (metrics == null) {
      metrics = defaultMetrics();
    }
  }

  private static Logging defaultLogging() {
    return new Logging(
        true,
        true,
        defaultOutput(),
        defaultOutput(),
        defaultOutput(),
        defaultMasking(),
        defaultRolling());
  }

  private static Output defaultOutput() {
    return new Output("console", "");
  }

  private static Masking defaultMasking() {
    return new Masking(true, List.of());
  }

  private static Rolling defaultRolling() {
    return new Rolling("50MB", 14, "1GB");
  }

  private static Correlation defaultCorrelation() {
    return new Correlation(
        CorrelationId.HEADER_NAME,
            CorrelationId.DEFAULT_VALIDATION_PATTERN,
            CorrelationId.DEFAULT_VALUE_TEMPLATE,
            "UTC",
            "invalidate-session",
            true);
  }

  private static Metrics defaultMetrics() {
    return new Metrics(true, true);
  }

  public record Logging(
      @DefaultValue("true") boolean enabled,
      @DefaultValue("true") boolean requestLoggingEnabled,
      @DefaultValue Output application,
      @DefaultValue Output telemetry,
      @DefaultValue Output security,
      @DefaultValue Masking masking,
      @DefaultValue Rolling rolling) {

    public Logging {
      if (application == null) {
        application = defaultOutput();
      }
      if (telemetry == null) {
        telemetry = defaultOutput();
      }
      if (security == null) {
        security = defaultOutput();
      }
      if (masking == null) {
        masking = defaultMasking();
      }
      if (rolling == null) {
        rolling = defaultRolling();
      }
    }
  }

  public record Output(@DefaultValue("console") String targets, @DefaultValue("") String file) {}

  public record Masking(
      @DefaultValue("true") boolean enabled, @DefaultValue List<String> additionalFields) {

    public Masking {
      additionalFields = additionalFields == null ? List.of() : List.copyOf(additionalFields);
    }
  }

  public record Rolling(
      @DefaultValue("50MB") String maxFileSize,
      @DefaultValue("14") int maxHistoryDays,
      @DefaultValue("1GB") String totalSizeCap) {}

  public record Correlation(
      @DefaultValue(CorrelationId.HEADER_NAME) String headerName,
      @DefaultValue(CorrelationId.DEFAULT_VALIDATION_PATTERN) String validationPattern,
      @DefaultValue(CorrelationId.DEFAULT_VALUE_TEMPLATE) String valueTemplate,
      @DefaultValue("UTC") String timeZone,
      @DefaultValue("invalidate-session") String invalidIdAction,
      @DefaultValue("true") boolean generateWhenMissing) {
  }

  public record Metrics(
      @DefaultValue("true") boolean enabled, @DefaultValue("true") boolean authMetricsEnabled) {}
}
