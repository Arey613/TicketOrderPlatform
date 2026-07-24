package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GreetingServiceTest {

    @Test
    void returnsDefaultGreeting() {
        assertThat(new GreetingService().getGreeting().message()).isEqualTo("Hello, World!");
    }
}
