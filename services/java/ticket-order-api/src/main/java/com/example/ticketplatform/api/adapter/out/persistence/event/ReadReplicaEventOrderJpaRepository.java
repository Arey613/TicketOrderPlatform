package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.infrastructure.config.persistence.ReadReplicaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

@ReadReplicaRepository
interface ReadReplicaEventOrderJpaRepository extends JpaRepository<EventOrderEntity, UUID> {

  List<EventOrderEntity> findByCustomerId(UUID customerId);
}
