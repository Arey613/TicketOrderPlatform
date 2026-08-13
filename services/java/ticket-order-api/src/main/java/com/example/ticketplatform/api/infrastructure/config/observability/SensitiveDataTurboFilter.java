package com.example.ticketplatform.api.infrastructure.config.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Marker;

public class SensitiveDataTurboFilter extends TurboFilter {

  private static final List<String> SENSITIVE_TOKENS =
      List.of(
          "password",
          "passwordhash",
          "password_hash",
          "csrftoken",
          "xsrf-token",
          "jsessionid",
          "cookie",
          "set-cookie",
          "authorization",
          "sessionid",
          "accesstoken",
          "refreshtoken");

  private boolean enabled = true;
  private List<String> additionalFields = List.of();

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setAdditionalFields(String additionalFields) {
    if (additionalFields == null || additionalFields.isBlank()) {
      this.additionalFields = List.of();
      return;
    }

    this.additionalFields =
        Arrays.stream(additionalFields.split(","))
            .map(String::trim)
            .filter(field -> !field.isBlank())
            .map(field -> field.toLowerCase(Locale.ROOT))
            .toList();
  }

  @Override
  public FilterReply decide(
      Marker marker,
      Logger logger,
      Level level,
      String format,
      Object[] params,
      Throwable throwable) {
    if (!enabled) {
      return FilterReply.NEUTRAL;
    }

    return containsSensitiveToken(format) || containsSensitiveParam(params)
        ? FilterReply.DENY
        : FilterReply.NEUTRAL;
  }

  private boolean containsSensitiveParam(Object[] params) {
    if (params == null) {
      return false;
    }

    for (Object param : params) {
      if (param != null && containsSensitiveToken(param.toString())) {
        return true;
      }
    }
    return false;
  }

  private boolean containsSensitiveToken(String value) {
    if (value == null) {
      return false;
    }

    String normalizedValue = value.toLowerCase(Locale.ROOT);
    return SENSITIVE_TOKENS.stream().anyMatch(normalizedValue::contains)
        || additionalFields.stream().anyMatch(normalizedValue::contains);
  }
}
