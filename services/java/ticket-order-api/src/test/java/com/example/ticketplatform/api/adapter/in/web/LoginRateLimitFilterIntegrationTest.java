package com.example.ticketplatform.api.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.infrastructure.config.security.LoginRateLimiter;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebControllerIntegrationTestConfiguration.class)
class LoginRateLimitFilterIntegrationTest {

  private static final UUID ENABLED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final User ENABLED_USER =
      WebControllerIntegrationTestConfiguration.user(
          ENABLED_USER_ID, "ratelimit@example.com", "{noop}secret", true);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestUsers testUsers;

  @Autowired
  private LoginRateLimiter loginRateLimiter;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(ENABLED_USER));
    loginRateLimiter.reset();
  }

  @Test
  void blocksLoginForEmailAfterFiveFailuresEvenWithCorrectPassword() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      login("ratelimit@example.com", "wrong").andExpect(status().isUnauthorized());
    }

    login("ratelimit@example.com", "secret").andExpect(status().isTooManyRequests());
  }

  @Test
  void blocksLoginForIpAfterFiveFailuresAcrossDifferentEmails() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      login("attempt" + attempt + "@example.com", "wrong").andExpect(status().isUnauthorized());
    }

    login("brand.new@example.com", "wrong").andExpect(status().isTooManyRequests());
  }

  @Test
  void successfulLoginResetsFailureCounters() throws Exception {
    for (int attempt = 0; attempt < 4; attempt++) {
      login("ratelimit@example.com", "wrong").andExpect(status().isUnauthorized());
    }

    login("ratelimit@example.com", "secret").andExpect(status().isOk());

    for (int attempt = 0; attempt < 4; attempt++) {
      login("ratelimit@example.com", "wrong").andExpect(status().isUnauthorized());
    }
  }

  private org.springframework.test.web.servlet.ResultActions login(String email, String password)
      throws Exception {
    Cookie csrfCookie = csrfCookie();
    return mockMvc.perform(
        post("/auth/login")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {
                  "login": "%s",
                  "password": "%s"
                }
                """
                    .formatted(email, password)));
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
