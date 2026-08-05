package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.LoginUseCase;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.api.AuthApi;
import com.example.ticketplatform.api.generated.contract.model.LoginRequest;
import com.example.ticketplatform.api.generated.contract.model.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class AuthController implements AuthApi {

  private final LoginUseCase loginUseCase;
  private final HttpServletRequest request;
  private final HttpServletResponse response;

  @Override
  public ResponseEntity<Void> getCsrfToken() {
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    try {
      User user = loginUseCase.login(loginRequest.getLogin(), loginRequest.getPassword());
      var authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              user.email(),
              null,
              List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      new HttpSessionSecurityContextRepository().saveContext(context, request, response);

      LoginResponse responseBody =
          new LoginResponse()
              .email(user.email())
              .role(
                  com.example.ticketplatform.api.generated.contract.model.UserRole.valueOf(
                      user.role().name()));
      return ResponseEntity.ok(responseBody);
    } catch (BadCredentialsException exception) {
      return ResponseEntity.status(401).build();
    }
  }

  @Override
  public ResponseEntity<Void> logout() {
    SecurityContextHolder.clearContext();
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
    return ResponseEntity.noContent().build();
  }
}
