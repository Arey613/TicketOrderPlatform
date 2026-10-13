package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.generated.contract.model.UpdateUserRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ControllerAdvice(assignableTypes = AdminController.class)
@RequiredArgsConstructor
class UserUpdateRequestBodyAdvice extends RequestBodyAdviceAdapter {

  private final ObjectMapper objectMapper;
  private final UserUpdateValidationProperties properties;

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return UpdateUserRequest.class.equals(targetType);
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType)
      throws IOException {
    byte[] body = inputMessage.getBody().readAllBytes();
    validateAllowedFields(body);
    return new CachedBodyHttpInputMessage(inputMessage.getHeaders(), body);
  }

  private void validateAllowedFields(byte[] body) throws IOException {
    JsonNode root = objectMapper.readTree(body);
    if (root == null || !root.isObject()) {
      throw new IllegalArgumentException("User update payload must be a JSON object");
    }

    Set<String> allowedFields = Set.copyOf(properties.getAllowedFields());
    Set<String> unsupportedFields =
        StreamSupport.stream(root.properties().spliterator(), false)
            .map(entry -> entry.getKey())
            .filter(field -> !allowedFields.contains(field))
            .collect(Collectors.toUnmodifiableSet());

    if (!unsupportedFields.isEmpty()) {
      throw new IllegalArgumentException("Unsupported user update fields: " + unsupportedFields);
    }
  }

  private record CachedBodyHttpInputMessage(HttpHeaders headers, byte[] body)
      implements HttpInputMessage {

    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }

    @Override
    public InputStream getBody() {
      return new ByteArrayInputStream(body);
    }
  }
}
