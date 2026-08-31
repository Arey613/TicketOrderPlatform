package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.CreateEventCommand;
import com.example.ticketplatform.api.application.port.in.CreateEventOrderCommand;
import com.example.ticketplatform.api.application.port.in.EventCommandUseCase;
import com.example.ticketplatform.api.application.port.in.EventQueryUseCase;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.in.UpdateEventCommand;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.application.port.out.UserCommandRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class EventService implements EventCommandUseCase, EventQueryUseCase {

  private final EventCommandRepositoryPort eventCommandRepositoryPort;
  private final EventQueryRepositoryPort eventQueryRepositoryPort;
  private final UserCommandRepositoryPort userCommandRepositoryPort;
  private final EventApplicationMapper eventApplicationMapper;
  private final Supplier<Instant> currentTimeSupplier;

  @Override
  @Transactional
  public Event createEvent(CreateEventCommand command) {
    User owner = getUser(command.ownerId());

    Instant now = currentTimeSupplier.get();
    Event event =
        eventApplicationMapper.toEvent(
            command,
            UUID.randomUUID(),
            eventApplicationMapper.toDetails(command.details(), UUID.randomUUID()),
            EventStatus.DRAFT,
            List.of(),
            now);

    return eventCommandRepositoryPort.save(event);
  }

  @Override
  @Transactional
  public Event updateEvent(UUID eventId, UUID userId, UpdateEventCommand command) {
    Event event = getOwnedEvent(eventId, userId);
    if (event.status() != EventStatus.DRAFT) {
      throw new IllegalStateException("Event cannot be updated from status " + event.status());
    }
    Instant now = currentTimeSupplier.get();

    return eventCommandRepositoryPort.save(
        eventApplicationMapper.toUpdatedEvent(
            event,
            command,
            eventApplicationMapper.toDetails(command.details(), event.details().id()),
            now));
  }

  @Override
  @Transactional
  public Event markEventAsPublished(UUID eventId, UUID userId) {
    Event event = getOwnedEvent(eventId, userId);
    if (event.status() != EventStatus.DRAFT) {
      throw new IllegalStateException("Event cannot be published from status " + event.status());
    }
    return updateStatus(event, EventStatus.PUBLISHED);
  }

  @Override
  @Transactional
  public Event markEventAsDraft(UUID eventId, UUID userId) {
    Event event = getOwnedEvent(eventId, userId);
    if (event.status() != EventStatus.PUBLISHED) {
      throw new IllegalStateException("Event cannot be unpublished from status " + event.status());
    }
    return updateStatus(event, EventStatus.DRAFT);
  }

  @Override
  @Transactional
  public List<EventOrder> createEventOrders(UUID userId, List<CreateEventOrderCommand> commands) {
    getUser(userId);
    Set<String> positions = new HashSet<>();

    for (CreateEventOrderCommand command : commands) {
      Event event = getEventForOrdering(command.eventId());
      String positionKey = command.eventId() + ":" + command.rowNumber() + ":" + command.placeNumber();

      if (!positions.add(positionKey)
          || eventQueryRepositoryPort.existsOrderPosition(
              event.id(), command.rowNumber(), command.placeNumber())) {
        throw new IllegalStateException("Event place is already reserved");
      }
    }

    Instant now = currentTimeSupplier.get();
    return eventCommandRepositoryPort.saveOrders(
        userId,
        commands.stream()
            .map(
                command ->
                    eventApplicationMapper.toOrder(command, UUID.randomUUID(), now))
            .toList());
  }

  @Override
  @Transactional
  public int deleteEventOrders(UUID userId, List<UUID> eventOrderIds) {
    List<EventOrder> orders = eventQueryRepositoryPort.findOrdersByIds(eventOrderIds);
    if (orders.size() != eventOrderIds.size()) {
      throw new NoSuchElementException("At least one event order was not found");
    }
    if (eventQueryRepositoryPort.findOrdersByIdsAndCustomerId(eventOrderIds, userId).size()
        != eventOrderIds.size()) {
      throw new SecurityException("User cannot delete at least one event order");
    }
    return Math.toIntExact(eventCommandRepositoryPort.deleteOrders(eventOrderIds));
  }

  @Override
  public Event getEvent(UUID eventId, UUID userId) {
    Event event =
        eventQueryRepositoryPort
            .findById(eventId)
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
    if (event.status() == EventStatus.PUBLISHED
        || (userId != null && event.ownerId().equals(userId))) {
      return event;
    }
    throw new SecurityException("User cannot read event: " + eventId);
  }

  @Override
  public PageResult<Event> listPublishedEvents(PageRequest pageRequest) {
    return eventQueryRepositoryPort.findPublished(pageRequest);
  }

  @Override
  public PageResult<Event> listOwnerEvents(UUID ownerId, PageRequest pageRequest) {
    return eventQueryRepositoryPort.findByOwnerId(ownerId, pageRequest);
  }

  @Override
  public PageResult<EventOrder> listUserOrders(UUID userId, PageRequest pageRequest) {
    return eventQueryRepositoryPort.findOrdersByCustomerId(userId, pageRequest);
  }

  private Event updateStatus(Event event, EventStatus status) {
    return eventCommandRepositoryPort.save(
        eventApplicationMapper.toEventWithStatus(event, status, currentTimeSupplier.get()));
  }

  private Event getOwnedEvent(UUID eventId, UUID userId) {
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

  private Event getEventForOrdering(UUID eventId) {
    Event event =
        eventCommandRepositoryPort
            .findById(eventId)
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
    if (event.status() != EventStatus.PUBLISHED) {
      throw new IllegalStateException("Event is not published: " + eventId);
    }
    return event;
  }

  private User getUser(UUID userId) {
    return userCommandRepositoryPort
        .findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
  }

}
