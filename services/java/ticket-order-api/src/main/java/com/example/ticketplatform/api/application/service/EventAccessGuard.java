package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.UserCommandRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EventAccessGuard {

  private final EventCommandRepositoryPort eventCommandRepositoryPort;
  private final UserCommandRepositoryPort userCommandRepositoryPort;

  Event requireOwnedEvent(UUID eventId, UUID userId) {
    User user = getUser(userId);
    Event event =
        eventCommandRepositoryPort
            .findById(eventId)
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
    if (!event.ownerId().equals(user.id())) {
      throw new SecurityException("User does not own event: " + eventId);
    }
    return event;
  }

  Event requireOwnedDraftEvent(UUID eventId, UUID userId) {
    Event event = requireOwnedEvent(eventId, userId);
    if (event.status() != EventStatus.DRAFT) {
      throw new IllegalStateException("Event is not a draft: " + eventId);
    }
    return event;
  }

  private User getUser(UUID userId) {
    return userCommandRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }
}
