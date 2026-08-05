package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.LoginCommand;
import com.example.ticketplatform.api.application.port.in.RegisterUserCommand;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.model.LoginRequest;
import com.example.ticketplatform.api.generated.contract.model.LoginResponse;
import com.example.ticketplatform.api.generated.contract.model.RegisterUserRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface AuthContractMapper {

  @Mapping(target = "rawPassword", source = "password")
  LoginCommand toCommand(LoginRequest request);

  @Mapping(target = "userEmail", source = "request.email")
  @Mapping(target = "passwordHash", source = "passwordHash")
  RegisterUserCommand toCommand(RegisterUserRequest request, String passwordHash);

  LoginResponse toLoginResponse(User user);
}
