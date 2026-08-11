package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.event.Event;
import java.util.List;
import java.util.UUID;

public interface EventCommandUseCase {

  Event createEvent(CreateEventCommand command);

  Event updateEvent(UUID eventId, UUID userId, UpdateEventCommand command);

  Event markEventAsPublished(UUID eventId, UUID userId);

  Event markEventAsDraft(UUID eventId, UUID userId);

  int createEventOrders(UUID userId, List<CreateEventOrderCommand> commands);

  int deleteEventOrders(UUID userId, List<UUID> eventOrderIds);
}
