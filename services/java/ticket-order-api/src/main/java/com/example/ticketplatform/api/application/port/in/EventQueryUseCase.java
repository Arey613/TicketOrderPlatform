package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.List;
import java.util.UUID;

public interface EventQueryUseCase {

  Event getEvent(UUID eventId, UUID userId);

  List<Event> listPublishedEvents();

  List<Event> listOwnerEvents(UUID ownerId);

  List<EventOrder> listUserOrders(UUID userId);
}
