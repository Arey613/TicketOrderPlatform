package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.event.Event;
import java.util.UUID;

public interface EventVideoUseCase {

  VideoUploadIssuance issueVideoUploadUrl(UUID eventId, UUID userId, IssueVideoUploadUrlCommand command);

  Event confirmVideoUpload(UUID eventId, UUID userId, ConfirmVideoUploadCommand command);
}
