package com.example.ticketplatform.api.application.port.out;

import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventQueryRepositoryPort {

  Optional<Event> findById(UUID id);

  PageResult<Event> findPublished(PageRequest pageRequest);

  PageResult<Event> findByOwnerId(UUID ownerId, PageRequest pageRequest);

  boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber);

  // TODO: Review bulk order lookup strategy if delete batches grow beyond small bounded lists.
  List<EventOrder> findOrdersByIds(Collection<UUID> ids);

  List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId);

  PageResult<EventOrder> findOrdersByCustomerId(UUID customerId, PageRequest pageRequest);
}
