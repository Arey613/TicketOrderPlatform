package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.UUID;

public interface EventQueryUseCase {

  Event getEvent(UUID eventId, UUID userId);

  PageResult<Event> listPublishedEvents(PageRequest pageRequest);

  PageResult<Event> listOwnerEvents(UUID ownerId, PageRequest pageRequest);

  PageResult<EventOrder> listUserOrders(UUID userId, PageRequest pageRequest);
}
