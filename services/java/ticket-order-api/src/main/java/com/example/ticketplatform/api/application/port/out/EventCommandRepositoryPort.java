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

  List<EventOrder> saveOrders(UUID customerId, List<EventOrder> orders);

  long deleteOrders(Collection<UUID> ids);
}
