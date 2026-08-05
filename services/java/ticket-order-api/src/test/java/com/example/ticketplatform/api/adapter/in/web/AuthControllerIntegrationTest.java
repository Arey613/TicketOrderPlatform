package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.user.User;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebControllerIntegrationTestConfiguration.class)
class AuthControllerIntegrationTest {

  private static final UUID ENABLED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID DISABLED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
  private static final User ENABLED_USER =
      WebControllerIntegrationTestConfiguration.user(
          ENABLED_USER_ID, "customer@example.com", "{noop}secret", true);
  private static final User DISABLED_USER =
      WebControllerIntegrationTestConfiguration.user(
          DISABLED_USER_ID, "disabled@example.com", "{noop}secret", false);

  @Autowired private MockMvc mockMvc;

  @Autowired private TestUsers testUsers;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(ENABLED_USER, DISABLED_USER));
  }

  @Test
  void exposesCsrfCookieWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/auth/csrf")).andExpect(status().isNoContent());

    assertThat(csrfCookie()).isNotNull();
  }

  @Test
  void rejectsLoginWhenEmailDoesNotExist() throws Exception {
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
                      "login": "missing@example.com",
                      "password": "secret"
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsLoginWhenPasswordIsWrong() throws Exception {
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
                      "login": "customer@example.com",
                      "password": "wrong"
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsLoginWhenUserIsDisabled() throws Exception {
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
                      "login": "disabled@example.com",
                      "password": "secret"
                    }
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatesLoginAndAllowsSessionBackedAccess() throws Exception {
    MvcResult loginResult =
        mockMvc.perform(validLogin()).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("customer@example.com"))
            .andExpect(jsonPath("$.role").value("CUSTOMER"))
            .andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
    SecurityContext securityContext =
        (SecurityContext)
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

    assertThat(securityContext.getAuthentication().getName()).isEqualTo("customer@example.com");
    assertThat(securityContext.getAuthentication().getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_CUSTOMER");

    mockMvc
        .perform(get("/users/{userId}", ENABLED_USER_ID).session(session))
        .andExpect(status().isOk());
  }

  @Test
  void logoutInvalidatesSessionBackedAccess() throws Exception {
    MvcResult loginResult = mockMvc.perform(validLogin()).andExpect(status().isOk()).andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

    Cookie csrfCookie = csrfCookie();

    mockMvc
        .perform(
            post("/auth/logout")
                .session(session)
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/users/{userId}", ENABLED_USER_ID).session(session))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutSucceedsWithoutCurrentSession() throws Exception {
    Cookie csrfCookie = csrfCookie();

    mockMvc
        .perform(
            post("/auth/logout").cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isNoContent());
  }

  @Test
  void rejectsLoginWithoutCsrfToken() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "login": "customer@example.com",
                      "password": "secret"
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  private org.springframework.test.web.servlet.RequestBuilder validLogin() throws Exception {
    Cookie csrfCookie = csrfCookie();
    return post("/auth/login")
        .cookie(csrfCookie)
        .header("X-XSRF-TOKEN", csrfCookie.getValue())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {
              "login": "customer@example.com",
              "password": "secret"
            }
            """);
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
