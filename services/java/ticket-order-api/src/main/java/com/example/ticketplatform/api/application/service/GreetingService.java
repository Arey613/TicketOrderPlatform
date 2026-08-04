package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.GetGreetingUseCase;
import com.example.ticketplatform.api.domain.model.Greeting;
import org.springframework.stereotype.Service;

@Service
class GreetingService implements GetGreetingUseCase {

  @Override
  public Greeting getGreeting() {
    return new Greeting("Hello, World!");
  }
}
