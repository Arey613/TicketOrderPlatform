package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.application.port.in.CreateEventCommand;
import com.example.ticketplatform.api.application.port.in.CreateEventOrderCommand;
import com.example.ticketplatform.api.application.port.in.EventDetailsCommand;
import com.example.ticketplatform.api.application.port.in.PageMetadata;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.in.UpdateEventCommand;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.application.port.out.UserCommandRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EventServiceTest {

  private static final Instant TEST_TIME = Instant.parse("2026-08-11T09:00:00Z");
  private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID OTHER_MANAGER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000004");
  private static final UUID EVENT_ORDER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000005");
  private static final UUID OTHER_EVENT_ORDER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000006");

  @Test
  void createsDraftEventForManager() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    EventService service = newService(events, List.of(user(MANAGER_ID, UserRole.MANAGER)));

    Event event =
        service.createEvent(
            new CreateEventCommand(
                MANAGER_ID,
                TEST_TIME,
                "Concert",
                "Main hall",
                "MUSIC",
                new EventDetailsCommand("Evening concert", 100, 10, 10)));

    assertThat(event.ownerId()).isEqualTo(MANAGER_ID);
    assertThat(event.status()).isEqualTo(EventStatus.DRAFT);
    assertThat(events.savedEvents).hasSize(1);
  }

  @Test
  void rejectsBulkOrdersAtomicallyWhenRequestDuplicatesASeat() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.PUBLISHED));
    EventService service =
        newService(events, List.of(user(CUSTOMER_ID, UserRole.CUSTOMER), user(MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(
            () ->
                service.createEventOrders(
                    CUSTOMER_ID,
                    List.of(
                        new CreateEventOrderCommand(EVENT_ID, 1, 1, "STANDARD"),
                        new CreateEventOrderCommand(EVENT_ID, 1, 1, "STANDARD"))))
        .isInstanceOf(IllegalStateException.class);

    assertThat(events.savedOrders).isEmpty();
  }

  @Test
  void rejectsBulkOrdersAtomicallyWhenRepositoryAlreadyHasReservedSeat() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.PUBLISHED));
    events.savedOrders.add(eventOrder(EVENT_ORDER_ID, CUSTOMER_ID, 1, 1));
    EventService service =
        newService(
            events,
            List.of(user(CUSTOMER_ID, UserRole.CUSTOMER), user(MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(
            () ->
                service.createEventOrders(
                    CUSTOMER_ID,
                    List.of(
                        new CreateEventOrderCommand(EVENT_ID, 2, 1, "STANDARD"),
                        new CreateEventOrderCommand(EVENT_ID, 1, 1, "STANDARD"))))
        .isInstanceOf(IllegalStateException.class);

    assertThat(events.savedOrders).hasSize(1);
  }

  @Test
  void rejectsOrderingUnpublishedEvent() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.DRAFT));
    EventService service =
        newService(
            events,
            List.of(user(CUSTOMER_ID, UserRole.CUSTOMER), user(MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(
            () ->
                service.createEventOrders(
                    CUSTOMER_ID,
                    List.of(new CreateEventOrderCommand(EVENT_ID, 1, 1, "STANDARD"))))
        .isInstanceOf(IllegalStateException.class);

    assertThat(events.savedOrders).isEmpty();
  }

  @Test
  void returnsCreatedOrders() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.PUBLISHED));
    EventService service =
        newService(events, List.of(user(CUSTOMER_ID, UserRole.CUSTOMER), user(MANAGER_ID, UserRole.MANAGER)));

    List<EventOrder> created =
        service.createEventOrders(
            CUSTOMER_ID,
            List.of(
                new CreateEventOrderCommand(EVENT_ID, 1, 1, "STANDARD"),
                new CreateEventOrderCommand(EVENT_ID, 1, 2, "STANDARD")));

    assertThat(created).hasSize(2);
    assertThat(created).extracting(EventOrder::customerId).containsOnly(CUSTOMER_ID);
    assertThat(events.savedOrders).hasSize(2);
  }

  @Test
  void deletesOrdersOwnedByCurrentUserAndReturnsDeletedCount() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.savedOrders.add(eventOrder(EVENT_ORDER_ID, CUSTOMER_ID, 1, 1));
    events.savedOrders.add(eventOrder(OTHER_EVENT_ORDER_ID, CUSTOMER_ID, 1, 2));
    EventService service = newService(events, List.of(user(CUSTOMER_ID, UserRole.CUSTOMER)));

    int deleted =
        service.deleteEventOrders(CUSTOMER_ID, List.of(EVENT_ORDER_ID, OTHER_EVENT_ORDER_ID));

    assertThat(deleted).isEqualTo(2);
    assertThat(events.savedOrders).isEmpty();
  }

  @Test
  void rejectsOrderDeleteWhenOneOrderBelongsToAnotherUser() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.savedOrders.add(eventOrder(EVENT_ORDER_ID, CUSTOMER_ID, 1, 1));
    events.savedOrders.add(eventOrder(OTHER_EVENT_ORDER_ID, OTHER_MANAGER_ID, 1, 2));
    EventService service = newService(events, List.of(user(CUSTOMER_ID, UserRole.CUSTOMER)));

    assertThatThrownBy(
            () ->
                service.deleteEventOrders(
                    CUSTOMER_ID, List.of(EVENT_ORDER_ID, OTHER_EVENT_ORDER_ID)))
        .isInstanceOf(SecurityException.class);

    assertThat(events.savedOrders).hasSize(2);
  }

  @Test
  void rejectsOrderDeleteWhenAnyOrderDoesNotExist() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.savedOrders.add(eventOrder(EVENT_ORDER_ID, CUSTOMER_ID, 1, 1));
    EventService service = newService(events, List.of(user(CUSTOMER_ID, UserRole.CUSTOMER)));

    assertThatThrownBy(
            () ->
                service.deleteEventOrders(
                    CUSTOMER_ID, List.of(EVENT_ORDER_ID, OTHER_EVENT_ORDER_ID)))
        .isInstanceOf(NoSuchElementException.class);

    assertThat(events.savedOrders).hasSize(1);
  }

  @Test
  void updatesOnlyOwnedEvents() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.DRAFT));
    EventService service =
        newService(
            events,
            List.of(
                user(MANAGER_ID, UserRole.MANAGER), user(OTHER_MANAGER_ID, UserRole.MANAGER)));

    Event updated =
        service.updateEvent(
            EVENT_ID,
            MANAGER_ID,
            new UpdateEventCommand(
                TEST_TIME.plusSeconds(3600),
                "Updated concert",
                "Updated hall",
                "THEATRE",
                new EventDetailsCommand("Updated details", 90, 9, 10)));

    assertThat(updated.name()).isEqualTo("Updated concert");
    assertThat(updated.ownerId()).isEqualTo(MANAGER_ID);
    assertThat(updated.status()).isEqualTo(EventStatus.DRAFT);
    assertThat(updated.details().description()).isEqualTo("Updated details");
    assertThat(updated.updatedAt()).isEqualTo(TEST_TIME);
  }

  @Test
  void rejectsUpdateWhenUserDoesNotOwnEvent() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.DRAFT));
    EventService service =
        newService(
            events,
            List.of(
                user(MANAGER_ID, UserRole.MANAGER), user(OTHER_MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(
            () ->
                service.updateEvent(
                    EVENT_ID,
                    OTHER_MANAGER_ID,
                    new UpdateEventCommand(
                        TEST_TIME.plusSeconds(3600),
                        "Updated concert",
                        "Updated hall",
                        "THEATRE",
                        new EventDetailsCommand("Updated details", 90, 9, 10))))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void rejectsUpdateWhenEventIsNotDraft() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.PUBLISHED));
    EventService service = newService(events, List.of(user(MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(
            () ->
                service.updateEvent(
                    EVENT_ID,
                    MANAGER_ID,
                    new UpdateEventCommand(
                        TEST_TIME.plusSeconds(3600),
                        "Updated concert",
                        "Updated hall",
                        "THEATRE",
                        new EventDetailsCommand("Updated details", 90, 9, 10))))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void marksOwnedDraftEventAsPublished() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.DRAFT));
    EventService service = newService(events, List.of(user(MANAGER_ID, UserRole.MANAGER)));

    Event published = service.markEventAsPublished(EVENT_ID, MANAGER_ID);

    assertThat(published.status()).isEqualTo(EventStatus.PUBLISHED);
    assertThat(published.updatedAt()).isEqualTo(TEST_TIME);
  }

  @Test
  void marksOwnedPublishedEventAsDraft() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.PUBLISHED));
    EventService service = newService(events, List.of(user(MANAGER_ID, UserRole.MANAGER)));

    Event draft = service.markEventAsDraft(EVENT_ID, MANAGER_ID);

    assertThat(draft.status()).isEqualTo(EventStatus.DRAFT);
    assertThat(draft.updatedAt()).isEqualTo(TEST_TIME);
  }

  @Test
  void rejectsPublishWhenUserDoesNotOwnEvent() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.DRAFT));
    EventService service =
        newService(
            events,
            List.of(
                user(MANAGER_ID, UserRole.MANAGER), user(OTHER_MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(() -> service.markEventAsPublished(EVENT_ID, OTHER_MANAGER_ID))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void rejectsPublishFromNonDraftStatus() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.PUBLISHED));
    EventService service = newService(events, List.of(user(MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(() -> service.markEventAsPublished(EVENT_ID, MANAGER_ID))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsUnpublishFromNonPublishedStatus() {
    TestEventRepositoryPort events = new TestEventRepositoryPort();
    events.events.add(event(EventStatus.DRAFT));
    EventService service = newService(events, List.of(user(MANAGER_ID, UserRole.MANAGER)));

    assertThatThrownBy(() -> service.markEventAsDraft(EVENT_ID, MANAGER_ID))
        .isInstanceOf(IllegalStateException.class);
  }

  private EventService newService(TestEventRepositoryPort events, List<User> users) {
    return new EventService(
        events,
        events,
        new TestUserRepositoryPort(users),
        Mappers.getMapper(EventApplicationMapper.class),
        Clock.fixed(TEST_TIME, ZoneOffset.UTC)::instant);
  }

  private static User user(UUID id, UserRole role) {
    return new User(id, id + "@example.com", "{noop}secret", role, true, TEST_TIME, TEST_TIME);
  }

  private static Event event(EventStatus status) {
    return Event.builder()
        .id(EVENT_ID)
        .ownerId(MANAGER_ID)
        .date(TEST_TIME)
        .name("Concert")
        .place("Main hall")
        .type("MUSIC")
        .status(status)
        .details(
            EventDetails.builder()
                .id(UUID.randomUUID())
                .description("Evening concert")
                .numberOfPlaces(100)
                .numberOfRows(10)
                .seatsPerRow(10)
                .build())
        .orders(List.of())
        .createdAt(TEST_TIME)
        .updatedAt(TEST_TIME)
        .build();
  }

  private static EventOrder eventOrder(
      UUID id, UUID customerId, Integer rowNumber, Integer placeNumber) {
    return EventOrder.builder()
        .id(id)
        .eventId(EVENT_ID)
        .customerId(customerId)
        .rowNumber(rowNumber)
        .placeNumber(placeNumber)
        .placeType("STANDARD")
        .reservationDate(TEST_TIME)
        .build();
  }

  private static class TestUserRepositoryPort implements UserCommandRepositoryPort {

    private final List<User> users;

    private TestUserRepositoryPort(List<User> users) {
      this.users = users;
    }

    @Override
    public User save(User user) {
      return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
      return users.stream().filter(user -> user.id().equals(id)).findFirst();
    }

  }

  private static class TestEventRepositoryPort
      implements EventCommandRepositoryPort, EventQueryRepositoryPort {

    private final List<Event> events = new ArrayList<>();
    private final List<Event> savedEvents = new ArrayList<>();
    private final List<EventOrder> savedOrders = new ArrayList<>();

    @Override
    public Event save(Event event) {
      savedEvents.add(event);
      events.removeIf(existing -> existing.id().equals(event.id()));
      events.add(event);
      return event;
    }

    @Override
    public Optional<Event> findById(UUID id) {
      return events.stream().filter(event -> event.id().equals(id)).findFirst();
    }

    @Override
    public PageResult<Event> findPublished(PageRequest pageRequest) {
      return page(
          events.stream().filter(event -> event.status() == EventStatus.PUBLISHED).toList(),
          pageRequest);
    }

    @Override
    public PageResult<Event> findByOwnerId(UUID ownerId, PageRequest pageRequest) {
      return page(
          events.stream().filter(event -> event.ownerId().equals(ownerId)).toList(),
          pageRequest);
    }

    @Override
    public boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber) {
      return savedOrders.stream()
          .anyMatch(
              order ->
                  order.eventId().equals(eventId)
                      && order.rowNumber() == rowNumber
                      && order.placeNumber() == placeNumber);
    }

    @Override
    public List<EventOrder> saveOrders(UUID customerId, List<EventOrder> orders) {
      List<EventOrder> ownedOrders =
          orders.stream()
              .map(
                  order ->
                      EventOrder.builder()
                          .id(order.id())
                          .eventId(order.eventId())
                          .customerId(customerId)
                          .rowNumber(order.rowNumber())
                          .placeNumber(order.placeNumber())
                          .placeType(order.placeType())
                          .reservationDate(order.reservationDate())
                          .eventName(order.eventName())
                          .eventDate(order.eventDate())
                          .build())
              .toList();
      savedOrders.addAll(ownedOrders);
      return ownedOrders;
    }

    @Override
    public PageResult<EventOrder> findOrdersByCustomerId(UUID customerId, PageRequest pageRequest) {
      return page(
          savedOrders.stream().filter(order -> customerId.equals(order.customerId())).toList(),
          pageRequest);
    }

    @Override
    public List<EventOrder> findOrdersByIds(Collection<UUID> ids) {
      return savedOrders.stream().filter(order -> ids.contains(order.id())).toList();
    }

    @Override
    public List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId) {
      return savedOrders.stream()
          .filter(order -> ids.contains(order.id()))
          .filter(order -> customerId.equals(order.customerId()))
          .toList();
    }

    @Override
    public long deleteOrders(Collection<UUID> ids) {
      long count = savedOrders.stream().filter(order -> ids.contains(order.id())).count();
      savedOrders.removeIf(order -> ids.contains(order.id()));
      return count;
    }

    private static <T> PageResult<T> page(List<T> items, PageRequest pageRequest) {
      int fromIndex = Math.min(pageRequest.page() * pageRequest.size(), items.size());
      int toIndex = Math.min(fromIndex + pageRequest.size(), items.size());
      return new PageResult<>(
          items.subList(fromIndex, toIndex),
          PageMetadata.of(pageRequest.page(), pageRequest.size(), items.size()));
    }
  }
}
