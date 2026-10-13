package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.out.UserSessionTrackingPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.infrastructure.config.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AuthenticationSessionManager {

  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final UserSessionTrackingPort userSessionTrackingPort;

  void authenticate(User user) {
    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            new AuthenticatedUserPrincipal(user.id(), user.email(), user.role()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    if (request.getSession(false) != null) {
      request.changeSessionId();
    }
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    HttpSession session = request.getSession(false);
    if (session != null) {
      userSessionTrackingPort.register(user.id(), session);
    }
  }

  void clear() {
    HttpSession session = request.getSession(false);
    if (session != null) {
      Object principal =
          SecurityContextHolder.getContext().getAuthentication() == null
              ? null
              : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
        userSessionTrackingPort.unregister(authenticatedUserPrincipal.userId(), session);
      }
    }
    SecurityContextHolder.clearContext();
    if (session != null) {
      session.invalidate();
    }
  }
}
