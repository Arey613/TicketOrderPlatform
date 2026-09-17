package com.example.ticketplatform.api.application.port.in;

import java.time.Instant;

public record PatchEventCommand(
    Instant date, String name, String place, String type, PatchEventDetailsCommand details) {}
