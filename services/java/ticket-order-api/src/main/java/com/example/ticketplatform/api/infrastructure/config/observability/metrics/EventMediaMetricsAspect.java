package com.example.ticketplatform.api.infrastructure.config.observability.metrics;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
class EventMediaMetricsAspect {

  private final TicketOrderMetrics ticketOrderMetrics;

  @Around(
      "execution(* com.example.ticketplatform.api.application.port.in.EventImageUseCase.attachEventImage(..))")
  Object recordImageAttachMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      Object result = joinPoint.proceed();
      ticketOrderMetrics.recordEventImageAttachAccepted();
      return result;
    } catch (RuntimeException exception) {
      ticketOrderMetrics.recordEventImageAttachRejected(exception.getClass().getSimpleName());
      throw exception;
    }
  }

  @Around(
      "execution(* com.example.ticketplatform.api.application.port.in.EventVideoUseCase.issueVideoUploadUrl(..))")
  Object recordVideoUploadUrlMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
      Object result = joinPoint.proceed();
      ticketOrderMetrics.recordEventVideoUploadUrlIssued();
      return result;
    } catch (RuntimeException exception) {
      ticketOrderMetrics.recordEventVideoUploadUrlRejected(exception.getClass().getSimpleName());
      throw exception;
    }
  }
}
