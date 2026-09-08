package com.example.ticketplatform.api.application.port.in;

import com.example.ticketplatform.api.domain.model.event.Event;
import java.util.UUID;

public interface EventImageUseCase {

  Event attachEventImage(UUID eventId, UUID userId, AttachEventImageCommand command);
}
