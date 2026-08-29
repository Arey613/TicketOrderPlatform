package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.infrastructure.config.persistence.JpaQueryCatalog;
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

  @PersistenceContext(unitName = "primary")
  private EntityManager primaryEntityManager;

  private final EventJpaRepository primaryEventRepository;
  private final EventMapper eventMapper;
  private final JpaQueryCatalog jpaQueryCatalog;
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
        () -> findPrimaryEventsByStatus(EventStatusEntity.PUBLISHED));
  }

  @Override
  public List<Event> findByOwnerId(UUID ownerId) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEntityManager
                .createQuery(
                    jpaQueryCatalog.get(EventQueryPersistenceAdapter.class, "findByOwnerId"),
                    EventEntity.class)
                .setParameter("ownerId", ownerId)
                .getResultStream()
                .map(eventMapper::toDomain)
                .toList(),
        () ->
            primaryEntityManager
                .createQuery(
                    jpaQueryCatalog.get(EventQueryPersistenceAdapter.class, "findByOwnerId"),
                    EventEntity.class)
                .setParameter("ownerId", ownerId)
                .getResultStream()
                .map(eventMapper::toDomain)
                .toList());
  }

  @Override
  public List<EventOrder> findOrdersByCustomerId(UUID customerId) {
    return readQueryExecutor.execute(
        () ->
            readReplicaEntityManager
                .createQuery(
                    jpaQueryCatalog.get(EventQueryPersistenceAdapter.class, "findOrdersByCustomerId"),
                    EventOrderEntity.class)
                .setParameter("customerId", customerId)
                .getResultStream()
                .map(eventMapper::toDomain)
                .toList(),
        () ->
            primaryEntityManager
                .createQuery(
                    jpaQueryCatalog.get(EventQueryPersistenceAdapter.class, "findOrdersByCustomerId"),
                    EventOrderEntity.class)
                .setParameter("customerId", customerId)
                .getResultStream()
                .map(eventMapper::toDomain)
                .toList());
  }

  private Optional<EventEntity> findReplicaEventById(UUID id) {
    return Optional.ofNullable(readReplicaEntityManager.find(EventEntity.class, id));
  }

  private List<Event> findReplicaEventsByStatus(EventStatusEntity status) {
    return readReplicaEntityManager
        .createQuery(
            jpaQueryCatalog.get(EventQueryPersistenceAdapter.class, "findPublished"),
            EventEntity.class)
        .setParameter("status", status)
        .getResultStream()
        .map(eventMapper::toDomain)
        .toList();
  }

  private List<Event> findPrimaryEventsByStatus(EventStatusEntity status) {
    return primaryEntityManager
        .createQuery(
            jpaQueryCatalog.get(EventQueryPersistenceAdapter.class, "findPublished"),
            EventEntity.class)
        .setParameter("status", status)
        .getResultStream()
        .map(eventMapper::toDomain)
        .toList();
  }
}
