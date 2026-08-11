package com.example.ticketplatform.api.domain.model.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record EventDetails(
    UUID id, String description, Integer numberOfPlaces, Integer numberOfRows, Integer seatsPerRow) {}
