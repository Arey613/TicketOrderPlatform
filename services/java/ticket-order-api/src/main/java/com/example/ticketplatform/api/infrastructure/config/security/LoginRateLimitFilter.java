package com.example.ticketplatform.api.infrastructure.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
class LoginRateLimitFilter extends OncePerRequestFilter {

  private static final String LOGIN_PATH = "/auth/login";

  private final LoginRateLimiter loginRateLimiter;
  private final ObjectMapper objectMapper;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !(HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    byte[] body = request.getInputStream().readAllBytes();
    CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request, body);
    String email = extractLogin(body);
    String ip = request.getRemoteAddr();

    boolean blockedByIp = loginRateLimiter.isBlockedByIp(ip);
    boolean blockedByEmail = loginRateLimiter.isBlockedByEmail(email);
    if (blockedByIp || blockedByEmail) {
      log.warn("auth.login.rate_limited limited_by={}", blockedByIp ? "ip" : "email");
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      return;
    }

    filterChain.doFilter(wrappedRequest, response);

    if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
      loginRateLimiter.recordFailure(ip, email);
    } else if (response.getStatus() == HttpStatus.OK.value()) {
      loginRateLimiter.recordSuccess(ip, email);
    }
  }

  private String extractLogin(byte[] body) {
    try {
      JsonNode node = objectMapper.readTree(body);
      JsonNode loginNode = node == null ? null : node.get("login");
      return loginNode == null ? null : loginNode.asText(null);
    } catch (JacksonException exception) {
      return null;
    }
  }
}
