package com.example.ticketplatform.api.domain.model.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record BookedPlace(
    UUID id,
    UUID eventId,
    UUID customerId,
    Integer rowNumber,
    Integer placeNumber,
    String placeType,
    Instant reservationDate,
    String eventName,
    Instant eventDate) {}
