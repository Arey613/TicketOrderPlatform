package com.example.ticketplatform.api.adapter.out.persistence.event;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventOrderJpaRepository extends JpaRepository<EventOrderEntity, UUID> {

  boolean existsByEventIdAndRowNumberAndPlaceNumber(UUID eventId, int rowNumber, int placeNumber);

  Page<EventOrderEntity> findByCustomerId(UUID customerId, Pageable pageable);

  List<EventOrderEntity> findByIdIn(Collection<UUID> ids);

  List<EventOrderEntity> findByIdInAndCustomerId(Collection<UUID> ids, UUID customerId);

  long deleteByIdIn(Collection<UUID> ids);
}
