package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class EventQueryPersistenceAdapter implements EventQueryRepositoryPort {

  private final EventJpaRepository primaryEventRepository;
  private final EventOrderJpaRepository primaryEventOrderRepository;
  private final ReadReplicaEventJpaRepository readReplicaEventRepository;
  private final ReadReplicaEventOrderJpaRepository readReplicaEventOrderRepository;
  private final EventMapper eventMapper;
  private final ReadQueryExecutor readQueryExecutor;

  @Override
  public Optional<Event> findById(UUID id) {
    return readQueryExecutor.execute(
        () -> readReplicaEventRepository.findById(id).map(eventMapper::toDomain),
        () -> primaryEventRepository.findById(id).map(eventMapper::toDomain));
  }

  @Override
  public List<Event> findPublished() {
    return readQueryExecutor.execute(
        () ->
            readReplicaEventRepository.findByStatus(EventStatusEntity.PUBLISHED).stream()
                .map(eventMapper::toDomain)
                .toList(),
        () ->
            primaryEventRepository.findByStatus(EventStatusEntity.PUBLISHED).stream()
                .map(eventMapper::toDomain)
                .toList());
  }

  @Override
  public List<Event> findByOwnerId(UUID ownerId) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEventRepository.findByOwnerId(ownerId).stream()
                .map(eventMapper::toDomain)
                .toList(),
        () ->
            primaryEventRepository.findByOwnerId(ownerId).stream().map(eventMapper::toDomain).toList());
  }

  @Override
  public boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEventOrderRepository.existsByEventIdAndRowNumberAndPlaceNumber(
                eventId, rowNumber, placeNumber),
        () ->
            primaryEventOrderRepository.existsByEventIdAndRowNumberAndPlaceNumber(
                eventId, rowNumber, placeNumber));
  }

  @Override
  public List<EventOrder> findOrdersByIds(Collection<UUID> ids) {
    return readQueryExecutor.execute(
        () -> readReplicaEventOrderRepository.findByIdIn(ids).stream()
            .map(eventMapper::toDomain)
            .toList(),
        () -> primaryEventOrderRepository.findByIdIn(ids).stream()
            .map(eventMapper::toDomain)
            .toList());
  }

  @Override
  public List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId) {
    return readQueryExecutor.execute(
        () -> readReplicaEventOrderRepository.findByIdInAndCustomerId(ids, customerId).stream()
            .map(eventMapper::toDomain)
            .toList(),
        () -> primaryEventOrderRepository.findByIdInAndCustomerId(ids, customerId).stream()
            .map(eventMapper::toDomain)
            .toList());
  }

  @Override
  public List<EventOrder> findOrdersByCustomerId(UUID customerId) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEventOrderRepository.findByCustomerId(customerId).stream()
                .map(eventMapper::toDomain)
                .toList(),
        () ->
            primaryEventOrderRepository.findByCustomerId(customerId).stream()
                .map(eventMapper::toDomain)
                .toList());
  }
}
