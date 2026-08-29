package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.infrastructure.config.persistence.ReadQueryExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class EventQueryPersistenceAdapter implements EventQueryRepositoryPort {

  @PersistenceContext(unitName = "readReplica")
  private EntityManager readReplicaEntityManager;

  private final EventJpaRepository primaryEventRepository;
  private final EventOrderJpaRepository primaryEventOrderRepository;
  private final EventMapper eventMapper;
  private final ReadQueryExecutor readQueryExecutor;

  @Override
  public Optional<Event> findById(UUID id) {
    return readQueryExecutor.execute(
        () -> findReplicaEventById(id).map(eventMapper::toDomain),
        () -> primaryEventRepository.findById(id).map(eventMapper::toDomain));
  }

  @Override
  public List<Event> findPublished() {
    return readQueryExecutor.execute(
        () -> findReplicaEventsByStatus(EventStatusEntity.PUBLISHED),
        () ->
            primaryEventRepository.findByStatus(EventStatusEntity.PUBLISHED).stream()
                .map(eventMapper::toDomain)
                .toList());
  }

  @Override
  public List<Event> findByOwnerId(UUID ownerId) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEntityManager
                .createQuery(
                    """
                    SELECT DISTINCT event
                    FROM EventEntity event
                    LEFT JOIN FETCH event.details
                    LEFT JOIN FETCH event.orders
                    WHERE event.ownerId = :ownerId
                    """,
                    EventEntity.class)
                .setParameter("ownerId", ownerId)
                .getResultStream()
                .map(eventMapper::toDomain)
                .toList(),
        () -> primaryEventRepository.findByOwnerId(ownerId).stream().map(eventMapper::toDomain).toList());
  }

  @Override
  public List<EventOrder> findOrdersByCustomerId(UUID customerId) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEntityManager
                .createQuery(
                    """
                    SELECT eventOrder
                    FROM EventOrderEntity eventOrder
                    JOIN FETCH eventOrder.event
                    WHERE eventOrder.customerId = :customerId
                    """,
                    EventOrderEntity.class)
                .setParameter("customerId", customerId)
                .getResultStream()
                .map(eventMapper::toDomain)
                .toList(),
        () ->
            primaryEventOrderRepository.findByCustomerId(customerId).stream()
                .map(eventMapper::toDomain)
                .toList());
  }

  private Optional<EventEntity> findReplicaEventById(UUID id) {
    return readReplicaEntityManager
        .createQuery(
            """
            SELECT DISTINCT event
            FROM EventEntity event
            LEFT JOIN FETCH event.details
            LEFT JOIN FETCH event.orders
            WHERE event.id = :id
            """,
            EventEntity.class)
        .setParameter("id", id)
        .getResultStream()
        .findFirst();
  }

  private List<Event> findReplicaEventsByStatus(EventStatusEntity status) {
    return readReplicaEntityManager
        .createQuery(
            """
            SELECT DISTINCT event
            FROM EventEntity event
            LEFT JOIN FETCH event.details
            LEFT JOIN FETCH event.orders
            WHERE event.status = :status
            """,
            EventEntity.class)
        .setParameter("status", status)
        .getResultStream()
        .map(eventMapper::toDomain)
        .toList();
  }
}
