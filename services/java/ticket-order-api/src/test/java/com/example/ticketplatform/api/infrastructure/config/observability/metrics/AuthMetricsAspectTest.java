package com.example.ticketplatform.api.infrastructure.config.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.infrastructure.config.observability.ObservabilityProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class AuthMetricsAspectTest {

  @Test
  void recordsLoginAttemptAndSuccess() throws Throwable {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    AuthMetricsAspect aspect =
        new AuthMetricsAspect(
            new TicketOrderMetrics(meterRegistry, new ObservabilityProperties(null, null, null)));
    Object expectedResult = new Object();

    Object result = aspect.recordLoginMetrics(new TestProceedingJoinPoint(expectedResult, null));

    assertThat(result).isSameAs(expectedResult);
    assertThat(meterRegistry.counter("ticket.auth.login.attempts").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter("ticket.auth.login.success", "result", "success").count())
        .isEqualTo(1.0);
  }

  @Test
  void recordsLoginAttemptAndInvalidCredentialsFailure() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    AuthMetricsAspect aspect =
        new AuthMetricsAspect(
            new TicketOrderMetrics(meterRegistry, new ObservabilityProperties(null, null, null)));
    BadCredentialsException exception = new BadCredentialsException("invalid");

    assertThatThrownBy(() -> aspect.recordLoginMetrics(new TestProceedingJoinPoint(null, exception)))
        .isSameAs(exception);

    assertThat(meterRegistry.counter("ticket.auth.login.attempts").count()).isEqualTo(1.0);
    assertThat(
            meterRegistry
                .counter(
                    "ticket.auth.login.failure",
                    "result",
                    "failure",
                    "reason",
                    "invalid_credentials")
                .count())
        .isEqualTo(1.0);
  }

  private record TestProceedingJoinPoint(Object result, Throwable throwable)
      implements ProceedingJoinPoint {

    @Override
    public Object proceed() throws Throwable {
      if (throwable != null) {
        throw throwable;
      }
      return result;
    }

    @Override
    public Object proceed(Object[] args) throws Throwable {
      return proceed();
    }

    @Override
    public void set$AroundClosure(AroundClosure arc) {}

    @Override
    public String toShortString() {
      return "";
    }

    @Override
    public String toLongString() {
      return "";
    }

    @Override
    public Object getThis() {
      return null;
    }

    @Override
    public Object getTarget() {
      return null;
    }

    @Override
    public Object[] getArgs() {
      return new Object[0];
    }

    @Override
    public Signature getSignature() {
      return null;
    }

    @Override
    public SourceLocation getSourceLocation() {
      return null;
    }

    @Override
    public String getKind() {
      return JoinPoint.METHOD_EXECUTION;
    }

    @Override
    public StaticPart getStaticPart() {
      return null;
    }
  }
}
