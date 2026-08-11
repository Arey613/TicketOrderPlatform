package com.example.ticketplatform.api.application.port.in;

import java.util.UUID;

public record CreateEventOrderCommand(
    UUID eventId, UUID customerReference, int rowNumber, int placeNumber, String placeType) {}
