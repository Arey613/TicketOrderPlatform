package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class EventPersistenceAdapter implements EventCommandRepositoryPort {

  private final EventJpaRepository eventRepository;
  private final EventOrderJpaRepository eventOrderRepository;
  private final EventMapper eventMapper;

  @Override
  public Event save(Event event) {
    return eventMapper.toDomain(eventRepository.save(eventMapper.toEntity(event)));
  }

  @Override
  public Optional<Event> findById(UUID id) {
    return eventRepository.findById(id).map(eventMapper::toDomain);
  }

  @Override
  public List<EventOrder> saveOrders(UUID customerId, List<EventOrder> orders) {
    return eventOrderRepository
        .saveAll(orders.stream().map(order -> toOrderEntity(customerId, order)).toList())
        .stream()
        .map(eventMapper::toDomain)
        .toList();
  }

  @Override
  public long deleteOrders(Collection<UUID> ids) {
    return eventOrderRepository.deleteByIdIn(ids);
  }

  private EventOrderEntity toOrderEntity(UUID customerId, EventOrder order) {
    EventEntity event = eventRepository.getReferenceById(order.eventId());
    return eventMapper.toEntity(order, event, customerId);
  }
}
