package com.example.ticketplatform.api.application.port.in;

import java.time.Instant;

public record UpdateEventCommand(
    Instant date, String name, String place, String type, EventDetailsCommand details) {}
