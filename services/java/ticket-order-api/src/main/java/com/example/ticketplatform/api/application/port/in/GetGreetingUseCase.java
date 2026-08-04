package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.Greeting;

public interface GetGreetingUseCase {

  Greeting getGreeting();
}
