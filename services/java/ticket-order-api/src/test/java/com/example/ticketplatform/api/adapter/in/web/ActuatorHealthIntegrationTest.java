package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.infrastructure.config.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebControllerIntegrationTestConfiguration.class)
class ActuatorHealthIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");
  private static final User USER =
      WebControllerIntegrationTestConfiguration.user(
          USER_ID, "actuator@example.com", "{noop}secret", true);

  @Autowired private MockMvc mockMvc;

  @Autowired private TestUsers testUsers;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(USER));
  }

  @Test
  void exposesHealthWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void protectsMetricsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
  }

  @Test
  void protectsPrometheusWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }

  @Test
  void exposesPrometheusWhenAuthenticated() throws Exception {
    mockMvc
        .perform(get("/actuator/prometheus").session(authenticatedSession()))
        .andExpect(status().isOk());
  }

  @Test
  void exposesCustomAuthMetricsThroughPrometheus() throws Exception {
    Cookie csrfCookie = csrfCookie();

    mockMvc
        .perform(
            post("/auth/login")
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "login": "actuator@example.com",
                      "password": "wrong"
                    }
                    """))
        .andExpect(status().isUnauthorized());

    String prometheus =
        mockMvc
            .perform(get("/actuator/prometheus").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(prometheus).contains("ticket_auth_login_attempts_total");
    assertThat(prometheus).contains("ticket_auth_login_failure_total");
  }

  private static MockHttpSession authenticatedSession() {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedUserPrincipal(USER_ID, "actuator@example.com", UserRole.CUSTOMER),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

  private Cookie csrfCookie() throws Exception {
    return mockMvc
        .perform(get("/auth/csrf"))
        .andExpect(status().isNoContent())
        .andReturn()
        .getResponse()
        .getCookie("XSRF-TOKEN");
  }
}
