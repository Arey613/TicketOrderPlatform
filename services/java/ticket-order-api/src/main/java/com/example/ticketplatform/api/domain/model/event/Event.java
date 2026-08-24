package com.example.ticketplatform.api.domain.model.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Event(
    UUID id,
    UUID ownerId,
    Instant date,
    String name,
    String place,
    String type,
    EventStatus status,
    EventDetails details,
    List<BookedPlace> orders,
    Instant createdAt,
    Instant updatedAt) {}
