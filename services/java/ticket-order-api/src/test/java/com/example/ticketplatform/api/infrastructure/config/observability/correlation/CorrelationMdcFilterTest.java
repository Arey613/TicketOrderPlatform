package com.example.ticketplatform.api.infrastructure.config.observability.correlation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

class CorrelationMdcFilterTest {

  @Test
  void runsBeforeSecurityFilters() {
    Order order = CorrelationMdcFilter.class.getAnnotation(Order.class);

    assertThat(order).isNotNull();
    assertThat(order.value()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }
}
