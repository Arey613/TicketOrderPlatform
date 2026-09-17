package com.example.ticketplatform.api.application.port.out;

import java.util.Map;

public record ObjectMetadata(Long contentLength, String contentType, Map<String, String> metadata) {}
