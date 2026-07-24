package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.GetGreetingUseCase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final GetGreetingUseCase getGreetingUseCase;

    public HelloController(GetGreetingUseCase getGreetingUseCase) {
        this.getGreetingUseCase = getGreetingUseCase;
    }

    @GetMapping("/hello")
    public String hello() {
        return getGreetingUseCase.getGreeting().message();
    }
}
