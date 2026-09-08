package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class EventControllerExceptionHandlerTest {

  private final EventControllerExceptionHandler handler = new EventControllerExceptionHandler();

  @Test
  void notFoundMapsNoSuchElementExceptionTo404() {
    assertThat(handler.notFound().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void forbiddenMapsSecurityExceptionTo403() {
    assertThat(handler.forbidden().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void badRequestMapsIllegalArgumentExceptionTo400() {
    assertThat(handler.badRequest(new IllegalArgumentException("bad")).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void conflictMapsIllegalStateExceptionTo409() {
    assertThat(handler.conflict().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void payloadTooLargeMapsMaxUploadSizeExceededExceptionTo413() {
    assertThat(handler.payloadTooLarge().getStatusCode())
        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
  }
}
