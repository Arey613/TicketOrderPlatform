package com.example.ticketplatform.api.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.function.Supplier;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "ticket-order-platform.observability.correlation.validation-pattern=^ticket-order-[0-9a-f]{8}-\\d{2}-\\d{2}-\\d{4}$",
      "ticket-order-platform.observability.correlation.value-template=ticket-order-{random-hex:8}-{date:dd-MM-yyyy}",
      "ticket-order-platform.observability.correlation.time-zone=Pacific/Kiritimati"
    })
@AutoConfigureMockMvc
class ConfigurableCorrelationTimeZoneIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void generatesCorrelationIdDateWithConfiguredTimeZone() throws Exception {
    mockMvc
        .perform(get("/auth/csrf"))
        .andExpect(status().isNoContent())
        .andExpect(
            header()
                .string(
                    "X-Correlation-ID",
                    Matchers.matchesPattern("ticket-order-[0-9a-f]{8}-14-08-2026")));
  }

  @TestConfiguration
  static class FixedTimeConfiguration {

    @Bean
    @Primary
    Supplier<Instant> fixedCurrentTimeSupplier() {
      return () -> Instant.parse("2026-08-13T22:30:00Z");
    }
  }
}
