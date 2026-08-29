package com.example.ticketplatform.api.adapter.out.persistence.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventPersistenceAdapterTest {

  private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000701");
  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000702");
  private static final UUID OTHER_OWNER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000703");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000704");
  private static final UUID DRAFT_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000705");
  private static final UUID EVENT_DETAILS_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000706");
  private static final UUID EVENT_ORDER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000707");
  private static final Instant EVENT_DATE = Instant.parse("2026-09-15T19:30:00Z");
  private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private EventPersistenceAdapter adapter;

  @BeforeEach
  void setUp() {
    insertUser(OWNER_ID, "event-owner@example.com", "MANAGER");
    insertUser(CUSTOMER_ID, "event-customer@example.com", "CUSTOMER");
    insertUser(OTHER_OWNER_ID, "other-event-owner@example.com", "MANAGER");
  }

  @Test
  void savesEventWithDetailsAndFindsItById() {
    adapter.save(event(EVENT_ID, OWNER_ID, EventStatus.PUBLISHED));
    entityManager.flush();
    entityManager.clear();

    Event found = adapter.findById(EVENT_ID).orElseThrow();

    assertThat(found.id()).isEqualTo(EVENT_ID);
    assertThat(found.ownerId()).isEqualTo(OWNER_ID);
    assertThat(found.status()).isEqualTo(EventStatus.PUBLISHED);
    assertThat(found.details().id()).isEqualTo(EVENT_DETAILS_ID);
    assertThat(found.details().numberOfPlaces()).isEqualTo(120);
    assertThat(found.orders()).isEmpty();
  }

  @Test
  void savesPublishedAndDraftEvents() {
    adapter.save(event(EVENT_ID, OWNER_ID, EventStatus.PUBLISHED));
    adapter.save(event(DRAFT_EVENT_ID, OTHER_OWNER_ID, EventStatus.DRAFT));
    entityManager.flush();
    entityManager.clear();

    assertThat(adapter.findById(EVENT_ID)).isPresent();
    assertThat(adapter.findById(DRAFT_EVENT_ID)).isPresent();
  }

  @Test
  void savesOrdersAndFindsReservedPositionsAndCustomerOrders() {
    adapter.save(event(EVENT_ID, OWNER_ID, EventStatus.PUBLISHED));
    EventOrder order = eventOrder(EVENT_ORDER_ID, EVENT_ID, 3, 7);

    List<EventOrder> saved = adapter.saveOrders(CUSTOMER_ID, List.of(order));
    entityManager.flush();
    entityManager.clear();

    assertThat(saved).hasSize(1);
    assertThat(adapter.existsOrderPosition(EVENT_ID, 3, 7)).isTrue();
    assertThat(adapter.existsOrderPosition(EVENT_ID, 3, 8)).isFalse();
    assertThat(adapter.findOrdersByIdsAndCustomerId(List.of(EVENT_ORDER_ID), CUSTOMER_ID))
        .singleElement()
        .satisfies(
            found -> {
              assertThat(found.id()).isEqualTo(EVENT_ORDER_ID);
              assertThat(found.eventId()).isEqualTo(EVENT_ID);
              assertThat(found.customerId()).isEqualTo(CUSTOMER_ID);
              assertThat(found.eventName()).isEqualTo("Event 704");
              assertThat(found.eventDate()).isEqualTo(EVENT_DATE);
              assertThat(found.placeType()).isEqualTo("VIP");
            });
  }

  @Test
  void findsAndDeletesOrdersByIds() {
    adapter.save(event(EVENT_ID, OWNER_ID, EventStatus.PUBLISHED));
    adapter.saveOrders(CUSTOMER_ID, List.of(eventOrder(EVENT_ORDER_ID, EVENT_ID, 3, 7)));
    entityManager.flush();
    entityManager.clear();

    assertThat(adapter.findOrdersByIds(List.of(EVENT_ORDER_ID)))
        .extracting(EventOrder::id)
        .containsExactly(EVENT_ORDER_ID);

    assertThat(adapter.deleteOrders(List.of(EVENT_ORDER_ID))).isEqualTo(1);
    entityManager.flush();
    entityManager.clear();

    assertThat(adapter.findOrdersByIds(List.of(EVENT_ORDER_ID))).isEmpty();
    assertThat(adapter.existsOrderPosition(EVENT_ID, 3, 7)).isFalse();
  }

  private void insertUser(UUID id, String email, String role) {
    entityManager
        .createNativeQuery(
            """
            INSERT INTO ticket_transactional.t_users
                (id, email, password_hash, role, enabled, created_at, updated_at)
            VALUES (:id, :email, '{noop}secret', :role, true, :createdAt, :updatedAt)
            """)
        .setParameter("id", id)
        .setParameter("email", email)
        .setParameter("role", role)
        .setParameter("createdAt", NOW)
        .setParameter("updatedAt", NOW)
        .executeUpdate();
  }

  private static Event event(UUID id, UUID ownerId, EventStatus status) {
    return Event.builder()
        .id(id)
        .ownerId(ownerId)
        .date(EVENT_DATE)
        .name("Event " + id.toString().substring(33))
        .place("Main hall")
        .type("MUSIC")
        .status(status)
        .details(
            EventDetails.builder()
                .id(eventDetailsId(id))
                .description("Large show")
                .numberOfPlaces(120)
                .numberOfRows(12)
                .seatsPerRow(10)
                .build())
        .orders(List.of())
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static EventOrder eventOrder(
      UUID id, UUID eventId, Integer rowNumber, Integer placeNumber) {
    return EventOrder.builder()
        .id(id)
        .eventId(eventId)
        .rowNumber(rowNumber)
        .placeNumber(placeNumber)
        .placeType("VIP")
        .reservationDate(NOW)
        .build();
  }

  private static UUID eventDetailsId(UUID eventId) {
    if (EVENT_ID.equals(eventId)) {
      return EVENT_DETAILS_ID;
    }
    return new UUID(0, eventId.getLeastSignificantBits() + 1000);
  }
}
