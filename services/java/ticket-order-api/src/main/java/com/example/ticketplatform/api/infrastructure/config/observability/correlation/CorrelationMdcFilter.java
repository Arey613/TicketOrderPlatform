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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
class CorrelationMdcFilter extends OncePerRequestFilter {

  private static final Logger securityLog =
      LoggerFactory.getLogger("com.example.ticketplatform.api.security");
  private static final String TRACEPARENT_HEADER = "traceparent";
  private static final String TRACE_ID_MDC_KEY = "trace_id";
  private static final String SPAN_ID_MDC_KEY = "span_id";
  private static final Pattern TRACEPARENT_PATTERN =
      Pattern.compile("^[\\da-f]{2}-([\\da-f]{32})-([\\da-f]{16})-[\\da-f]{2}$");

  private final Supplier<Instant> currentTimeSupplier;
  private final ObservabilityProperties observabilityProperties;

  private record CorrelationDecision(
      String headerName, String correlationId, boolean rejected, boolean missing) {}

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();
    CorrelationDecision correlation = decideCorrelation(request);

    response.setHeader(correlation.headerName(), correlation.correlationId());
    populateRequestMdc(request, correlation.correlationId());

    try {
      if (correlation.rejected()) {
        rejectRequestForInvalidCorrelation(request, response, correlation.missing());
        return;
      }

      filterChain.doFilter(request, response);
    } finally {
      addAuthenticationMdc();
      logRequestCompletion(request, response, startedAt);
      MDC.clear();
    }
  }

  private CorrelationDecision decideCorrelation(HttpServletRequest request) {
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

    return new CorrelationDecision(
        correlationHeaderName, correlationId, rejectedCorrelationId, missingCorrelationId);
  }

  private void populateRequestMdc(HttpServletRequest request, String correlationId) {
    MDC.put(CorrelationId.MDC_KEY, correlationId);
    MDC.put("requestMethod", request.getMethod());
    MDC.put("requestPath", request.getRequestURI());
    populateTraceMdc(request);
  }

  private void populateTraceMdc(HttpServletRequest request) {
    if (MDC.get(TRACE_ID_MDC_KEY) != null && MDC.get(SPAN_ID_MDC_KEY) != null) {
      return;
    }

    String traceparent = request.getHeader(TRACEPARENT_HEADER);
    if (traceparent == null) {
      return;
    }

    Matcher matcher = TRACEPARENT_PATTERN.matcher(traceparent);
    if (!matcher.matches()) {
      return;
    }

    MDC.put(TRACE_ID_MDC_KEY, matcher.group(1));
    MDC.put(SPAN_ID_MDC_KEY, matcher.group(2));
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

  private void rejectRequestForInvalidCorrelation(
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

  private void logRequestCompletion(
      HttpServletRequest request, HttpServletResponse response, long startedAt) {
    if (!observabilityProperties.logging().enabled()
        || !observabilityProperties.logging().requestLoggingEnabled()) {
      return;
    }

    log.warn(
        "http.request.completed method={} path={} status={} durationMs={} authenticated={}",
        request.getMethod(),
        request.getRequestURI(),
        response.getStatus(),
        elapsedMillis(startedAt),
        isRealAuthentication(SecurityContextHolder.getContext().getAuthentication()));
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
