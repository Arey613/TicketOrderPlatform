package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebControllerIntegrationTestConfiguration.class)
class AdminControllerIntegrationTest {

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000001001");
  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000001002");
  private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000001003");
  private static final User ADMIN =
      WebControllerIntegrationTestConfiguration.user(
          ADMIN_ID, "admin.manage@example.com", "{noop}secret", UserRole.ADMIN, true);
  private static final User CUSTOMER =
      WebControllerIntegrationTestConfiguration.user(
          CUSTOMER_ID, "customer.manage@example.com", "{noop}secret", UserRole.CUSTOMER, true);
  private static final User MANAGER =
      WebControllerIntegrationTestConfiguration.user(
          MANAGER_ID, "manager.manage@example.com", "{noop}secret", UserRole.MANAGER, true);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestUsers testUsers;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(ADMIN, CUSTOMER, MANAGER));
  }

  @Test
  void rejectsAdminUserListWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/admin/manage/users")).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsAdminUserListForCustomer() throws Exception {
    mockMvc
        .perform(get("/admin/manage/users").session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void listsUsersForAdminIncludingAdminAccounts() throws Exception {
    mockMvc
        .perform(get("/admin/manage/users").session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[?(@.role == 'ADMIN')]").exists())
        .andExpect(jsonPath("$.page.totalElements").value(3));
  }

  @Test
  void filtersUsersByRoleEnabledAndSearch() throws Exception {
    mockMvc
        .perform(
            get("/admin/manage/users")
                .queryParam("role", "MANAGER")
                .queryParam("enabled", "true")
                .queryParam("search", "manager")
                .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(MANAGER_ID.toString()));
  }

  @Test
  void rejectsUnsupportedUserSort() throws Exception {
    mockMvc
        .perform(
            get("/admin/manage/users")
                .queryParam("sort", "name,asc")
                .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returnsAnyUserDetailsForAdmin() throws Exception {
    mockMvc
        .perform(
            get("/admin/manage/users/{userId}", ADMIN_ID)
                .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ADMIN_ID.toString()))
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void returnsNotFoundForMissingUser() throws Exception {
    mockMvc
        .perform(
            get("/admin/manage/users/{userId}", UUID.fromString("00000000-0000-0000-0000-000000009999"))
                .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void updatesManageableUserRoleAndEnabledState() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                put("/admin/manage/users/{userId}", CUSTOMER_ID)
                    .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "role": "MANAGER",
                          "enabled": false
                        }
                        """)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(CUSTOMER_ID.toString()))
        .andExpect(jsonPath("$.role").value("MANAGER"))
        .andExpect(jsonPath("$.enabled").value(false));

    User updated = testUsers.findById(CUSTOMER_ID).orElseThrow();
    assertThat(updated.role()).isEqualTo(UserRole.MANAGER);
    assertThat(updated.enabled()).isFalse();
  }

  @Test
  void rejectsAdminRoleAssignment() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                put("/admin/manage/users/{userId}", CUSTOMER_ID)
                    .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "role": "ADMIN",
                          "enabled": true
                        }
                        """)))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsAdminTargetUpdate() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                put("/admin/manage/users/{userId}", ADMIN_ID)
                    .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "role": "MANAGER",
                          "enabled": true
                        }
                        """)))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsUpdateMissingEnabled() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                put("/admin/manage/users/{userId}", CUSTOMER_ID)
                    .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "role": "MANAGER"
                        }
                        """)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsUpdateWithUnsupportedField() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                put("/admin/manage/users/{userId}", CUSTOMER_ID)
                    .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "role": "MANAGER",
                          "enabled": true,
                          "email": "changed@example.com"
                        }
                        """)))
        .andExpect(status().isBadRequest());
  }

  private static MockHttpSession authenticatedSession(String email, String role) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            email, null, List.of(new SimpleGrantedAuthority(role))));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

  private static MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) {
    Cookie csrfCookie = new Cookie("XSRF-TOKEN", "test-csrf-token");
    return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
  }
}
