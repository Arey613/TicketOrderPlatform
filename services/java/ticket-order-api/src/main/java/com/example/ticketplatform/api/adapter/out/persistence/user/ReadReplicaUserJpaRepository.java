package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.infrastructure.config.persistence.ReadReplicaRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

@ReadReplicaRepository
interface ReadReplicaUserJpaRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByEmail(String email);
}
