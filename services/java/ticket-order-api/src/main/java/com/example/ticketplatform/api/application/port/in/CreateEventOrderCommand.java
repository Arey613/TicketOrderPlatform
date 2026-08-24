package com.example.ticketplatform.api.application.port.in;

import java.util.UUID;

public record CreateEventOrderCommand(
    UUID eventId, Integer rowNumber, Integer placeNumber, String placeType) {}
