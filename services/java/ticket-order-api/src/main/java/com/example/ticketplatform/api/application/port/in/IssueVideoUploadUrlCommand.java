package com.example.ticketplatform.api.application.port.in;

public record IssueVideoUploadUrlCommand(
    String fileName, String contentType, Long fileSizeBytes, String sha256) {}
