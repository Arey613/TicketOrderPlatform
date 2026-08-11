package com.example.ticketplatform.api.adapter.out.persistence.event;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

  @EntityGraph(attributePaths = {"details", "orders"})
  List<EventEntity> findByStatus(EventStatusEntity status);

  @EntityGraph(attributePaths = {"details", "orders"})
  List<EventEntity> findByOwnerId(UUID ownerId);
}
