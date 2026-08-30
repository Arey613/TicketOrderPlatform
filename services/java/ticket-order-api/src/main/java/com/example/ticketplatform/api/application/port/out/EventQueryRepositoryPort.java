package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventQueryRepositoryPort {

  Optional<Event> findById(UUID id);

  List<Event> findPublished();

  List<Event> findByOwnerId(UUID ownerId);

  boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber);

  long countOrdersByIds(Collection<UUID> ids);

  long countOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId);

  List<EventOrder> findOrdersByCustomerId(UUID customerId);
}
