package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.infrastructure.config.security.AuthenticatedUserPrincipal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebControllerIntegrationTestConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ObservabilityIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
  private static final User USER =
      WebControllerIntegrationTestConfiguration.user(
          USER_ID, "observability@example.com", "{noop}secret", true);
  private static final String VALID_CORRELATION_ID =
      "018f0f5e-4e7a-7a89-b2f3-5d9d4a0b91c2-13-08-2026";
  private static final String TRACEPARENT =
      "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

  @Autowired private MockMvc mockMvc;

  @Autowired private TestUsers testUsers;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(USER));
    MDC.clear();
  }

  @Test
  void generatesCorrelationIdWhenHeaderIsMissing() throws Exception {
    String correlationId =
        mockMvc
            .perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Correlation-ID"))
            .andReturn()
            .getResponse()
            .getHeader("X-Correlation-ID");

    assertThat(correlationId)
        .matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}-\\d{2}-\\d{2}-\\d{4}");
  }

  @Test
  void reusesValidCorrelationId() throws Exception {
    mockMvc
        .perform(get("/actuator/health").header("X-Correlation-ID", VALID_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Correlation-ID", VALID_CORRELATION_ID));
  }

  @Test
  void rejectsCorrelationIdOutsideConfiguredPattern() throws Exception {
    mockMvc
        .perform(
            get("/actuator/health")
                .header(
                    "X-Correlation-ID",
                    "018f0f5e-4e7a-7a89-b2f3-5d9d4a0b91c2-2026-08-13"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("X-Correlation-ID"));
  }

  @Test
  void invalidCorrelationIdInvalidatesSessionAndRequiresRelogin() throws Exception {
    MockHttpSession session = authenticatedSession();

    mockMvc
        .perform(
            get("/users/{userId}", USER_ID)
                .session(session)
                .header("X-Correlation-ID", "wrong-format"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("X-Correlation-ID"));

    assertThatThrownBy(() -> session.getAttribute("anything"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void blankCorrelationIdInvalidatesSessionAndRequiresRelogin() throws Exception {
    MockHttpSession session = authenticatedSession();

    mockMvc
        .perform(get("/users/{userId}", USER_ID).session(session).header("X-Correlation-ID", " "))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("X-Correlation-ID"));

    assertThatThrownBy(() -> session.getAttribute("anything"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void clearsMdcAfterRequestCompletion() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());

    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  void emitsJsonLogWithCorrelationMdc(CapturedOutput output) throws Exception {
    mockMvc
        .perform(get("/actuator/health").header("X-Correlation-ID", VALID_CORRELATION_ID))
        .andExpect(status().isOk());

    String requestLog =
        output.getOut().lines()
            .filter(line -> line.contains("http.request.completed"))
            .reduce((first, second) -> second)
            .orElseThrow();

    assertThat(requestLog).startsWith("{").endsWith("}");
    assertThat(requestLog).contains("\"level\":\"WARN\"");
    assertThat(requestLog).contains("\"mdc\"");
    assertThat(requestLog).contains("\"correlationId\":\"" + VALID_CORRELATION_ID + "\"");
    assertThat(requestLog).doesNotContain("XSRF-TOKEN", "JSESSIONID", "password");
  }

  @Test
  void extractsTraceContextIntoJsonLogMdc(CapturedOutput output) throws Exception {
    mockMvc
        .perform(
            get("/actuator/health")
                .header("X-Correlation-ID", VALID_CORRELATION_ID)
                .header("traceparent", TRACEPARENT))
        .andExpect(status().isOk());

    String requestLog =
        output.getOut().lines()
            .filter(line -> line.contains("http.request.completed"))
            .reduce((first, second) -> second)
            .orElseThrow();

    assertThat(requestLog).contains("\"trace_id\":\"4bf92f3577b34da6a3ce929d0e0e4736\"");
    assertThat(requestLog).contains("\"span_id\"");
  }

  private static MockHttpSession authenticatedSession() {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedUserPrincipal(USER_ID, "observability@example.com", USER.role()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }
}
