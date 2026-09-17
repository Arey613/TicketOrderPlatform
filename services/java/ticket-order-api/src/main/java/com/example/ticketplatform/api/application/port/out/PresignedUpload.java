package com.example.ticketplatform.api.application.port.out;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public record PresignedUpload(
    URI uploadUrl, Map<String, String> requiredHeaders, Instant expiresAt, URI publicUrl) {}
