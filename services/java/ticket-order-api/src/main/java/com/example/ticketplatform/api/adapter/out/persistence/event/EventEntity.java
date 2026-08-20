package com.example.ticketplatform.api.adapter.out.persistence.event;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t_event", schema = "ticket_transactional")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class EventEntity {

  @Id
  @Column(name = "event_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "date", nullable = false)
  private Instant date;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "place", nullable = false, length = 200)
  private String place;

  @Column(name = "type", nullable = false, length = 100)
  private String type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private EventStatusEntity status;

  @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private EventDetailsEntity details;

  @OneToMany(mappedBy = "event", fetch = FetchType.EAGER)
  private List<EventOrderEntity> orders = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  void attachDetails(EventDetailsEntity details) {
    this.details = details;
    details.setEvent(this);
  }
}
