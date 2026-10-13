package com.example.ticketplatform.api.adapter.in.web;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticket-order-platform.user-update")
@Getter
@Setter
class UserUpdateValidationProperties {

  private List<String> allowedFields = List.of();
}
