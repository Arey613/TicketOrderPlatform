package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.GetUserUseCase;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.api.UsersApi;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class UserController implements UsersApi {

  private final GetUserUseCase getUserUseCase;
  private final UserContractMapper userContractMapper;

  @Override
  public ResponseEntity<UserResponse> getUser(UUID userId) {
    try {
      User user = getUserUseCase.getUser(userId);
      return ResponseEntity.ok(userContractMapper.toContract(user));
    } catch (NoSuchElementException exception) {
      return ResponseEntity.notFound().build();
    }
  }
}
