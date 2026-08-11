package com.example.ticketplatform.api.application.port.in;

import java.time.Instant;
import java.util.UUID;

public record CreateEventCommand(
    UUID ownerId, Instant date, String name, String place, String type, EventDetailsCommand details) {}
