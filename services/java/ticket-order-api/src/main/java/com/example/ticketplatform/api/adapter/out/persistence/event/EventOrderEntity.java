package com.example.ticketplatform.api.adapter.out.persistence.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "t_event_order", schema = "ticket_transactional")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
class EventOrderEntity {

  @Id
  @Column(name = "event_order_id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @Column(name = "customer_reference")
  private UUID customerReference;

  @Column(name = "row_number", nullable = false)
  private Integer rowNumber;

  @Column(name = "place_number", nullable = false)
  private Integer placeNumber;

  @Column(name = "place_type", nullable = false, length = 100)
  private String placeType;

  @Column(name = "reservation_date", nullable = false)
  private Instant reservationDate;
}
