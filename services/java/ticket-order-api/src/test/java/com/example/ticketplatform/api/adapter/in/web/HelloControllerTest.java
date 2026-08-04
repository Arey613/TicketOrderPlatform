package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.domain.model.Greeting;
import org.junit.jupiter.api.Test;

class HelloControllerTest {

  @Test
  void returnsHelloWorld() {
    HelloController controller = new HelloController(() -> new Greeting("Hello, World!"));

    assertThat(controller.hello()).isEqualTo("Hello, World!");
  }
}
