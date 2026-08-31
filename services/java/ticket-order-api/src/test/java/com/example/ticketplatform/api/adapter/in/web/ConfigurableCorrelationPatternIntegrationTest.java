package com.example.ticketplatform.api.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "ticket-order-platform.observability.correlation.validation-pattern=^ticket-order-[0-9a-f]{8}$",
      "ticket-order-platform.observability.correlation.value-template=ticket-order-{random-hex:8}"
    })
@AutoConfigureMockMvc
class ConfigurableCorrelationPatternIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void acceptsCorrelationIdMatchingConfiguredPattern() throws Exception {
    mockMvc
        .perform(get("/auth/csrf").header("X-Correlation-ID", "ticket-order-1234abcd"))
        .andExpect(status().isNoContent())
        .andExpect(header().string("X-Correlation-ID", "ticket-order-1234abcd"));
  }

  @Test
  void generatesCorrelationIdFromConfiguredTemplate() throws Exception {
    mockMvc
        .perform(get("/auth/csrf"))
        .andExpect(status().isNoContent())
        .andExpect(
            header()
                .string("X-Correlation-ID", Matchers.matchesPattern("ticket-order-[0-9a-f]{8}")));
  }
}
