package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.CreateEventCommand;
import com.example.ticketplatform.api.application.port.in.CreateEventOrderCommand;
import com.example.ticketplatform.api.application.port.in.EventCommandUseCase;
import com.example.ticketplatform.api.application.port.in.EventQueryUseCase;
import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class WebControllerIntegrationTestConfiguration {

  private static final Instant TEST_TIME = Instant.parse("2026-08-05T00:00:00Z");

  @Bean
  @Primary
  UserRepositoryPort userRepositoryPort(TestUsers testUsers) {
    return testUsers;
  }

  @Bean
  TestUsers testUsers() {
    return new TestUsers();
  }

  @Bean
  @Primary
  TestEvents testEvents() {
    return new TestEvents();
  }

  static User user(UUID id, String email, String passwordHash, boolean enabled) {
    return new User(id, email, passwordHash, UserRole.CUSTOMER, enabled, TEST_TIME, TEST_TIME);
  }

  static User user(UUID id, String email, String passwordHash, UserRole role, boolean enabled) {
    return new User(id, email, passwordHash, role, enabled, TEST_TIME, TEST_TIME);
  }

  static class TestUsers implements UserRepositoryPort {

    private final Map<UUID, User> usersById = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();

    @Override
    public User save(User user) {
      usersById.put(user.id(), user);
      usersByEmail.put(user.email(), user);
      return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
      return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
      return Optional.ofNullable(usersByEmail.get(email));
    }

    @Override
    public boolean existsByEmail(String email) {
      return usersByEmail.containsKey(email);
    }

    void reset(List<User> users) {
      usersById.clear();
      usersByEmail.clear();
      for (User user : users) {
        save(user);
      }
    }
  }

  static class TestEvents implements EventCommandUseCase, EventQueryUseCase {

    private final Map<UUID, Event> eventsById = new HashMap<>();
    private final List<EventOrder> orders = new ArrayList<>();
    private UUID lastCommandUserId;
    private int createdOrderCount;
    private int deletedOrderCount;

    @Override
    public Event createEvent(CreateEventCommand command) {
      Event event =
          Event.builder()
              .id(UUID.fromString("00000000-0000-0000-0000-000000000501"))
              .ownerId(command.ownerId())
              .date(command.date())
              .name(command.name())
              .place(command.place())
              .type(command.type())
              .status(EventStatus.DRAFT)
              .details(
                  com.example.ticketplatform.api.domain.model.event.EventDetails.builder()
                      .id(UUID.fromString("00000000-0000-0000-0000-000000000502"))
                      .description(command.details().description())
                      .numberOfPlaces(command.details().numberOfPlaces())
                      .numberOfRows(command.details().numberOfRows())
                      .seatsPerRow(command.details().seatsPerRow())
                      .build())
              .orders(List.of())
              .createdAt(TEST_TIME)
              .updatedAt(TEST_TIME)
              .build();
      eventsById.put(event.id(), event);
      lastCommandUserId = command.ownerId();
      return event;
    }

    @Override
    public Event updateEvent(UUID eventId, UUID userId, com.example.ticketplatform.api.application.port.in.UpdateEventCommand command) {
      lastCommandUserId = userId;
      Event existing = getEvent(eventId, userId);
      Event updated =
          Event.builder()
              .id(existing.id())
              .ownerId(existing.ownerId())
              .date(command.date())
              .name(command.name())
              .place(command.place())
              .type(command.type())
              .status(existing.status())
              .details(existing.details())
              .orders(existing.orders())
              .createdAt(existing.createdAt())
              .updatedAt(TEST_TIME)
              .build();
      eventsById.put(updated.id(), updated);
      return updated;
    }

    @Override
    public Event markEventAsPublished(UUID eventId, UUID userId) {
      lastCommandUserId = userId;
      return withStatus(eventId, EventStatus.PUBLISHED);
    }

    @Override
    public Event markEventAsDraft(UUID eventId, UUID userId) {
      lastCommandUserId = userId;
      return withStatus(eventId, EventStatus.DRAFT);
    }

    @Override
    public int createEventOrders(UUID userId, List<CreateEventOrderCommand> commands) {
      lastCommandUserId = userId;
      createdOrderCount = commands.size();
      return createdOrderCount;
    }

    @Override
    public int deleteEventOrders(UUID userId, List<UUID> eventOrderIds) {
      lastCommandUserId = userId;
      deletedOrderCount = eventOrderIds.size();
      return deletedOrderCount;
    }

    @Override
    public Event getEvent(UUID eventId, UUID userId) {
      return eventsById.get(eventId);
    }

    @Override
    public List<Event> listPublishedEvents() {
      return eventsById.values().stream()
          .filter(event -> event.status() == EventStatus.PUBLISHED)
          .toList();
    }

    @Override
    public List<Event> listOwnerEvents(UUID ownerId) {
      return eventsById.values().stream().filter(event -> event.ownerId().equals(ownerId)).toList();
    }

    @Override
    public List<EventOrder> listUserOrders(UUID userId) {
      return orders.stream().filter(order -> userId.equals(order.customerReference())).toList();
    }

    void reset(List<Event> events, List<EventOrder> orders) {
      eventsById.clear();
      this.orders.clear();
      events.forEach(event -> eventsById.put(event.id(), event));
      this.orders.addAll(orders);
      lastCommandUserId = null;
      createdOrderCount = 0;
      deletedOrderCount = 0;
    }

    UUID lastCommandUserId() {
      return lastCommandUserId;
    }

    int createdOrderCount() {
      return createdOrderCount;
    }

    int deletedOrderCount() {
      return deletedOrderCount;
    }

    private Event withStatus(UUID eventId, EventStatus status) {
      Event event = eventsById.get(eventId);
      Event updated =
          Event.builder()
              .id(event.id())
              .ownerId(event.ownerId())
              .date(event.date())
              .name(event.name())
              .place(event.place())
              .type(event.type())
              .status(status)
              .details(event.details())
              .orders(event.orders())
              .createdAt(event.createdAt())
              .updatedAt(TEST_TIME)
              .build();
      eventsById.put(updated.id(), updated);
      return updated;
    }
  }
}
