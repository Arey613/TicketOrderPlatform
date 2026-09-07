package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.event.Event;
import java.net.URI;
import java.time.Instant;
import java.util.Map;

public record VideoUploadIssuance(
    Event event,
    String videoUrl,
    URI uploadUrl,
    Map<String, String> requiredHeaders,
    Instant expiresAt) {}
