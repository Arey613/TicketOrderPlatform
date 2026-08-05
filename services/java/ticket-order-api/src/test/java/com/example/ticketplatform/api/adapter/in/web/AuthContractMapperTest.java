package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.application.port.in.LoginCommand;
import com.example.ticketplatform.api.application.port.in.RegisterUserCommand;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.generated.contract.model.LoginRequest;
import com.example.ticketplatform.api.generated.contract.model.LoginResponse;
import com.example.ticketplatform.api.generated.contract.model.RegisterUserRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AuthContractMapperTest {

  private final AuthContractMapper mapper = Mappers.getMapper(AuthContractMapper.class);

  @Test
  void mapsLoginRequestToCommand() {
    LoginRequest request = new LoginRequest().login("customer@example.com").password("secret");

    LoginCommand command = mapper.toCommand(request);

    assertThat(command.login()).isEqualTo("customer@example.com");
    assertThat(command.rawPassword()).isEqualTo("secret");
  }

  @Test
  void mapsRegisterRequestToCommandWithPasswordHash() {
    RegisterUserRequest request =
        new RegisterUserRequest().email("new@example.com").password("raw-secret");

    RegisterUserCommand command = mapper.toCommand(request, "{bcrypt}hash");

    assertThat(command.userEmail()).isEqualTo("new@example.com");
    assertThat(command.passwordHash()).isEqualTo("{bcrypt}hash");
    assertThat(command.passwordHash()).doesNotContain("raw-secret");
  }

  @Test
  void mapsDomainUserToLoginResponse() {
    User user =
        new User(
            UUID.fromString("00000000-0000-0000-0000-000000000501"),
            "manager@example.com",
            "{noop}secret",
            UserRole.MANAGER,
            true,
            Instant.parse("2026-08-05T00:00:00Z"),
            Instant.parse("2026-08-05T00:00:00Z"));

    LoginResponse response = mapper.toLoginResponse(user);

    assertThat(response.getEmail()).isEqualTo("manager@example.com");
    assertThat(response.getRole().getValue()).isEqualTo("MANAGER");
  }
}
