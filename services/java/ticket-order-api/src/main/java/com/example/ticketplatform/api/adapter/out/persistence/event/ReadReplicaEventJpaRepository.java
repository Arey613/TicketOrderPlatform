package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.infrastructure.config.persistence.ReadReplicaRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@ReadReplicaRepository
interface ReadReplicaEventJpaRepository extends JpaRepository<EventEntity, UUID> {

  Page<EventEntity> findByStatus(EventStatusEntity status, Pageable pageable);

  Page<EventEntity> findByOwnerId(UUID ownerId, Pageable pageable);
}
