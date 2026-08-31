package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.infrastructure.config.persistence.ReadReplicaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@ReadReplicaRepository
interface ReadReplicaEventOrderJpaRepository extends JpaRepository<EventOrderEntity, UUID> {

  Page<EventOrderEntity> findByCustomerId(UUID customerId, Pageable pageable);

  boolean existsByEventIdAndRowNumberAndPlaceNumber(UUID eventId, int rowNumber, int placeNumber);

  List<EventOrderEntity> findByIdIn(Collection<UUID> ids);

  List<EventOrderEntity> findByIdInAndCustomerId(Collection<UUID> ids, UUID customerId);
}
