package com.example.ticketplatform.api.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class UserControllerIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final User USER =
      WebControllerIntegrationTestConfiguration.user(
          USER_ID, "reader@example.com", "{noop}secret", true);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestUsers testUsers;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(USER));
  }

  @Test
  void rejectsUserRequestWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/users/{userId}", USER_ID)).andExpect(status().isUnauthorized());
  }

  @Test
  void returnsUserWhenAuthenticated() throws Exception {
    mockMvc
        .perform(get("/users/{userId}", USER_ID).session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.email").value("reader@example.com"))
        .andExpect(jsonPath("$.role").value("CUSTOMER"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  private static MockHttpSession authenticatedSession() {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            "reader@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

}
