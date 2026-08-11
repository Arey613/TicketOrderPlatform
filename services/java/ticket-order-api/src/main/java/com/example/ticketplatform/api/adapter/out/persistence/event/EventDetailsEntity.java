package com.example.ticketplatform.api.adapter.out.persistence.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t_event_details")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class EventDetailsEntity {

  @Id
  @Column(name = "event_details_id", nullable = false, updatable = false)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @Column(name = "description", nullable = false)
  private String description;

  @Column(name = "number_of_places", nullable = false)
  private Integer numberOfPlaces;

  @Column(name = "number_of_rows", nullable = false)
  private Integer numberOfRows;

  @Column(name = "seats_per_row", nullable = false)
  private Integer seatsPerRow;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
