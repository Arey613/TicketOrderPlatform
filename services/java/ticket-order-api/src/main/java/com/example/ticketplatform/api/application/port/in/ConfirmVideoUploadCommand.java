package com.example.ticketplatform.api.application.port.in;

import java.net.URI;

public record ConfirmVideoUploadCommand(
    URI videoUrl, String contentType, Long fileSizeBytes, String sha256) {}
