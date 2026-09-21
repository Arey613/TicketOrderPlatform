package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.infrastructure.config.persistence.ReadReplicaRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

@ReadReplicaRepository
interface ReadReplicaEventOrderJpaRepository extends JpaRepository<EventOrderEntity, UUID> {

  @Query(
      """
      select eventOrder
      from EventOrderEntity eventOrder
      join eventOrder.event event
      where eventOrder.customerId = :customerId
        and event.date >= :currentTime
      """)
  Page<EventOrderEntity> findUpcomingByCustomerId(
      UUID customerId,
      Instant currentTime,
      Pageable pageable);

  boolean existsByEventIdAndRowNumberAndPlaceNumber(UUID eventId, int rowNumber, int placeNumber);

  List<EventOrderEntity> findByIdIn(Collection<UUID> ids);

  List<EventOrderEntity> findByIdInAndCustomerId(Collection<UUID> ids, UUID customerId);
}
