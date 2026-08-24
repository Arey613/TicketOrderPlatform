package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepositoryPort {

  Event save(Event event);

  Optional<Event> findById(UUID id);

  List<Event> findPublished();

  List<Event> findByOwnerId(UUID ownerId);

  boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber);

  List<EventOrder> saveOrders(UUID customerId, List<EventOrder> orders);

  List<EventOrder> findOrdersByCustomerId(UUID customerId);

  List<EventOrder> findOrdersByIds(Collection<UUID> ids);

  // TODO TICKET_PORTAL#3: Review whether this port method stays required once order deletion
  // can be expressed as one ownership-scoped repository operation.
  List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId);

  long deleteOrders(Collection<UUID> ids);
}
