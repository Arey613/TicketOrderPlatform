package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.LoginUseCase;
import com.example.ticketplatform.api.application.port.in.UserCommandUseCase;
import com.example.ticketplatform.api.application.port.out.PasswordHasherPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.api.AuthApi;
import com.example.ticketplatform.api.generated.contract.model.LoginRequest;
import com.example.ticketplatform.api.generated.contract.model.LoginResponse;
import com.example.ticketplatform.api.generated.contract.model.RegisterUserRequest;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
class AuthController implements AuthApi {

  private final LoginUseCase loginUseCase;
  private final UserCommandUseCase userCommandUseCase;
  private final PasswordHasherPort passwordHasherPort;
  private final AuthContractMapper authContractMapper;
  private final UserContractMapper userContractMapper;
  private final AuthenticationSessionManager authenticationSessionManager;

  @Override
  public ResponseEntity<Void> getCsrfToken() {
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    try {
      User user = loginUseCase.login(authContractMapper.toCommand(loginRequest));
      authenticationSessionManager.authenticate(user);

      return ResponseEntity.ok(authContractMapper.toLoginResponse(user));
    } catch (BadCredentialsException exception) {
      log.warn("auth.login.failed reason=invalid_credentials");
      return ResponseEntity.status(401).build();
    }
  }

  @Override
  public ResponseEntity<Void> logout() {
    authenticationSessionManager.clear();
    log.warn("auth.logout.completed");
    return ResponseEntity.noContent().build();
  }

  // TODO: duplicate-email registration currently surfaces as an unhandled exception (raw 500)
  // instead of 409 Conflict. Needs a @RestControllerAdvice mapping to the contract's
  // ErrorResponse schema.
  @Override
  public ResponseEntity<UserResponse> registerUser(RegisterUserRequest registerUserRequest) {
    String passwordHash = passwordHasherPort.hash(registerUserRequest.getPassword());
    User user =
        userCommandUseCase.registerUser(
            authContractMapper.toCommand(registerUserRequest, passwordHash));
    authenticationSessionManager.authenticate(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(userContractMapper.toContract(user));
  }
}
