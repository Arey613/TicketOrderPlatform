package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.EventQueryUseCase;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.generated.contract.model.CreateEventOrderItem;
import com.example.ticketplatform.api.generated.contract.model.CreateEventOrdersRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EventOrderRequestValidator {

  private final EventQueryUseCase eventQueryUseCase;

  void validate(UUID userId, CreateEventOrdersRequest request) {
    request.getOrders().forEach(item -> validate(userId, item));
  }

  private void validate(UUID userId, CreateEventOrderItem item) {
    Event event = eventQueryUseCase.getEvent(item.getEventId(), userId);
    if (item.getRow() > event.details().numberOfRows()) {
      throw new IllegalArgumentException("Event order row is outside event capacity");
    }
    if (item.getPlace() > event.details().seatsPerRow()) {
      throw new IllegalArgumentException("Event order place is outside event capacity");
    }
  }
}
