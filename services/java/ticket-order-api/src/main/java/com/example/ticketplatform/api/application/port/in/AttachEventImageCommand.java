package com.example.ticketplatform.api.application.port.in;

public record AttachEventImageCommand(
    byte[] imageData, String originalFilename, String declaredContentType) {}
