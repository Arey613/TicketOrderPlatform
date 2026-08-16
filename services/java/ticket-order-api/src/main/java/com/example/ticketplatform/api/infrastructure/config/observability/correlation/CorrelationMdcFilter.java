package com.example.ticketplatform.api.infrastructure.config.observability.correlation;

import com.example.ticketplatform.api.infrastructure.config.observability.ObservabilityProperties;
import com.example.ticketplatform.api.infrastructure.config.security.AuthenticatedUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
class CorrelationMdcFilter extends OncePerRequestFilter {

  private static final Logger securityLog =
      LoggerFactory.getLogger("com.example.ticketplatform.api.security");

  private final Supplier<Instant> currentTimeSupplier;
  private final ObservabilityProperties observabilityProperties;

  CorrelationMdcFilter(
      Supplier<Instant> currentTimeSupplier, ObservabilityProperties observabilityProperties) {
    this.currentTimeSupplier = currentTimeSupplier;
    this.observabilityProperties = observabilityProperties;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();
    String correlationHeaderName = observabilityProperties.correlation().headerName();
    String incomingCorrelationId = request.getHeader(correlationHeaderName);
    boolean missingCorrelationId = incomingCorrelationId == null || incomingCorrelationId.isBlank();
    boolean invalidCorrelationId =
        incomingCorrelationId != null
            && !CorrelationId.isValid(
                incomingCorrelationId, observabilityProperties.correlation().validationPattern());
    boolean rejectedCorrelationId =
        invalidCorrelationId
            || (missingCorrelationId && !observabilityProperties.correlation().generateWhenMissing());
    String correlationId =
        rejectedCorrelationId
            ? CorrelationId.generate(
                currentTimeSupplier.get(),
                observabilityProperties.correlation().valueTemplate(),
                correlationZoneId())
            : effectiveCorrelationId(incomingCorrelationId);

    response.setHeader(correlationHeaderName, correlationId);
    MDC.put(CorrelationId.MDC_KEY, correlationId);
    MDC.put("requestMethod", request.getMethod());
    MDC.put("requestPath", request.getRequestURI());

    try {
      if (rejectedCorrelationId) {
        rejectCorrelationId(request, response, missingCorrelationId);
        return;
      }

      filterChain.doFilter(request, response);
    } finally {
      addAuthenticationMdc();
      if (observabilityProperties.logging().enabled()
          && observabilityProperties.logging().requestLoggingEnabled()) {
        log.warn(
            "http.request.completed method={} path={} status={} durationMs={} authenticated={}",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            elapsedMillis(startedAt),
            isRealAuthentication(SecurityContextHolder.getContext().getAuthentication()));
      }
      MDC.clear();
    }
  }

  private String effectiveCorrelationId(String incomingCorrelationId) {
    if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
      return CorrelationId.generate(
          currentTimeSupplier.get(),
          observabilityProperties.correlation().valueTemplate(),
          correlationZoneId());
    }
    return incomingCorrelationId;
  }

  private ZoneId correlationZoneId() {
    return ZoneId.of(observabilityProperties.correlation().timeZone());
  }

  private void rejectCorrelationId(
      HttpServletRequest request, HttpServletResponse response, boolean missingCorrelationId)
      throws IOException {
    HttpSession session = request.getSession(false);
    if (session != null && shouldInvalidateSession()) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    String eventName = missingCorrelationId ? "http.correlation.missing" : "http.correlation.invalid";
    securityLog.warn("{} status={}", eventName, HttpStatus.UNAUTHORIZED.value());
  }

  private boolean shouldInvalidateSession() {
    return "invalidate-session"
        .equalsIgnoreCase(observabilityProperties.correlation().invalidIdAction());
  }

  private void addAuthenticationMdc() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!isRealAuthentication(authentication)) {
      return;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal) {
      MDC.put("authenticatedUserId", authenticatedUserPrincipal.userId().toString());
    }
    authentication.getAuthorities().stream()
        .findFirst()
        .ifPresent(authority -> MDC.put("authenticatedUserRole", authority.getAuthority()));
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }

  private boolean isRealAuthentication(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
