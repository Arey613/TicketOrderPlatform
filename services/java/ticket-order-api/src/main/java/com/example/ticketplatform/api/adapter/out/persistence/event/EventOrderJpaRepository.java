package com.example.ticketplatform.api.adapter.out.persistence.event;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface EventOrderJpaRepository extends JpaRepository<EventOrderEntity, UUID> {

  boolean existsByEventIdAndRowNumberAndPlaceNumber(UUID eventId, int rowNumber, int placeNumber);

  List<EventOrderEntity> findByCustomerReference(UUID customerReference);

  List<EventOrderEntity> findByIdIn(Collection<UUID> ids);

  long deleteByIdIn(Collection<UUID> ids);
}
