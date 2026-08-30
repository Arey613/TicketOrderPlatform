package com.example.ticketplatform.api.adapter.out.persistence.event;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventOrderJpaRepository extends JpaRepository<EventOrderEntity, UUID> {

  boolean existsByEventIdAndRowNumberAndPlaceNumber(UUID eventId, int rowNumber, int placeNumber);

  List<EventOrderEntity> findByCustomerId(UUID customerId);

  long countByIdIn(Collection<UUID> ids);

  long countByIdInAndCustomerId(Collection<UUID> ids, UUID customerId);

  long deleteByIdIn(Collection<UUID> ids);
}
