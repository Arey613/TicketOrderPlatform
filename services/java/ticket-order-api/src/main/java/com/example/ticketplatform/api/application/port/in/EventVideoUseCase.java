package com.example.ticketplatform.api.application.port.in;

import java.util.UUID;

public interface EventVideoUseCase {

  VideoUploadIssuance issueVideoUploadUrl(UUID eventId, UUID userId, IssueVideoUploadUrlCommand command);
}
