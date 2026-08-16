package com.example.ticketplatform.api.infrastructure.config.observability.metrics;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
class AuthMetricsAspect {

  private final TicketOrderMetrics ticketOrderMetrics;

  @Around("execution(* com.example.ticketplatform.api.application.port.in.LoginUseCase.login(..))")
  Object recordLoginMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
    ticketOrderMetrics.recordLoginAttempt();
    try {
      Object result = joinPoint.proceed();
      ticketOrderMetrics.recordLoginSuccess();
      return result;
    } catch (BadCredentialsException exception) {
      ticketOrderMetrics.recordLoginFailure("invalid_credentials");
      throw exception;
    }
  }
}
