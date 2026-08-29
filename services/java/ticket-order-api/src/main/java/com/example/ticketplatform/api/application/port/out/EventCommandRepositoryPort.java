package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCommandRepositoryPort {

  Event save(Event event);

  Optional<Event> findById(UUID id);

  boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber);

  List<EventOrder> saveOrders(UUID customerId, List<EventOrder> orders);

  List<EventOrder> findOrdersByIds(Collection<UUID> ids);

  List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId);

  long deleteOrders(Collection<UUID> ids);
}
