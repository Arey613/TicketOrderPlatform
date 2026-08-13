package com.example.ticketplatform.api.infrastructure.config.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

class LogTargetFilterTest {

  @Test
  void acceptsConfiguredTarget() {
    LogTargetFilter filter = new LogTargetFilter();
    filter.setTargets("console,file");
    filter.setTarget("file");

    assertThat(filter.decide(null)).isEqualTo(FilterReply.NEUTRAL);
  }

  @Test
  void deniesUnconfiguredTarget() {
    LogTargetFilter filter = new LogTargetFilter();
    filter.setTargets("console");
    filter.setTarget("file");

    assertThat(filter.decide(null)).isEqualTo(FilterReply.DENY);
  }

  @Test
  void deniesConfiguredTargetWhenDisabled() {
    LogTargetFilter filter = new LogTargetFilter();
    filter.setEnabled(false);
    filter.setTargets("console,file");
    filter.setTarget("file");

    assertThat(filter.decide(null)).isEqualTo(FilterReply.DENY);
  }
}
