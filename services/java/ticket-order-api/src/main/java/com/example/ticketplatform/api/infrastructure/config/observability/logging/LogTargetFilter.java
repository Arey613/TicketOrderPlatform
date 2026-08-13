package com.example.ticketplatform.api.infrastructure.config.observability.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class LogTargetFilter extends Filter<ILoggingEvent> {

  private String targets = "console";
  private String target;
  private boolean enabled = true;

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setTargets(String targets) {
    this.targets = targets;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  @Override
  public FilterReply decide(ILoggingEvent event) {
    if (!enabled) {
      return FilterReply.DENY;
    }

    if (target == null || target.isBlank()) {
      return FilterReply.DENY;
    }

    return configuredTargets().contains(normalize(target)) ? FilterReply.NEUTRAL : FilterReply.DENY;
  }

  private Set<String> configuredTargets() {
    return Arrays.stream(targets.split(","))
        .map(this::normalize)
        .filter(value -> !value.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  private String normalize(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
