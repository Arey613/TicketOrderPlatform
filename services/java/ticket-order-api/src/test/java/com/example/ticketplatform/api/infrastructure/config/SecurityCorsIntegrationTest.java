package com.example.ticketplatform.api.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "ticket-order-platform.cors.allowed-origins=http://localhost:5173",
      "ticket-order-platform.cors.allowed-methods=GET,OPTIONS",
      "ticket-order-platform.cors.allowed-headers=*",
      "ticket-order-platform.cors.allow-credentials=false",
      "ticket-order-platform.cors.max-age=3600"
    })
class SecurityCorsIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void permitsHelloEndpointWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/hello")).andExpect(status().isOk());
  }

  @Test
  void appliesCorsHeadersToAllowedPreflightRequest() throws Exception {
    mockMvc
        .perform(
            options("/hello")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(header().string("Access-Control-Allow-Methods", "GET,OPTIONS"))
        .andExpect(header().string("Access-Control-Max-Age", "3600"));
  }
}
