package com.example.ticketplatform.api.adapter.out.persistence.event;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

  Optional<EventEntity> findByIdWithOrders(UUID id);

  Page<EventEntity> findByStatus(EventStatusEntity status, Pageable pageable);

  Page<EventEntity> findByOwnerId(UUID ownerId, Pageable pageable);
}
