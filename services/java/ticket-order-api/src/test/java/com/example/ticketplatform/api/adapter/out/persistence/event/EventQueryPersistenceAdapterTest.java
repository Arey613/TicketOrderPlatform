package com.example.ticketplatform.api.adapter.out.persistence.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class EventQueryPersistenceAdapterTest {

  private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
  private static final UUID CUSTOMER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000802");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000803");
  private static final UUID DRAFT_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000804");
  private static final UUID EVENT_DETAILS_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000805");
  private static final UUID EVENT_ORDER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000806");
  private static final Instant EVENT_DATE = Instant.parse("2026-09-15T19:30:00Z");
  private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

  @Autowired
  private DataSource primaryDataSource;

  @Autowired
  private PlatformTransactionManager primaryTransactionManager;

  @Autowired
  private EventPersistenceAdapter commandAdapter;

  @Autowired
  private EventQueryPersistenceAdapter queryAdapter;

  @BeforeEach
  void setUp() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(primaryDataSource);
    jdbcTemplate.update("DELETE FROM ticket_transactional.t_event_order");
    jdbcTemplate.update("DELETE FROM ticket_transactional.t_event_details");
    jdbcTemplate.update("DELETE FROM ticket_transactional.t_event");
    jdbcTemplate.update("DELETE FROM ticket_transactional.t_users");
    insertUser(jdbcTemplate, OWNER_ID, "event-query-owner@example.com", "MANAGER");
    insertUser(jdbcTemplate, CUSTOMER_ID, "event-query-customer@example.com", "CUSTOMER");
  }

  @Test
  void readsPublishedEventsByNamedQuery() {
    save(event(EVENT_ID, EventStatus.PUBLISHED));
    save(event(DRAFT_EVENT_ID, EventStatus.DRAFT));

    assertThat(queryAdapter.findPublished()).extracting(Event::id).containsExactly(EVENT_ID);
  }

  @Test
  void readsOwnerEventsByNamedQuery() {
    save(event(EVENT_ID, EventStatus.PUBLISHED));

    assertThat(queryAdapter.findByOwnerId(OWNER_ID)).extracting(Event::id).containsExactly(EVENT_ID);
  }

  @Test
  void readsCustomerOrdersByNamedQuery() {
    save(event(EVENT_ID, EventStatus.PUBLISHED));
    saveOrders(CUSTOMER_ID, List.of(eventOrder(EVENT_ORDER_ID, EVENT_ID)));

    assertThat(queryAdapter.findOrdersByCustomerId(CUSTOMER_ID))
        .singleElement()
        .satisfies(
            found -> {
              assertThat(found.id()).isEqualTo(EVENT_ORDER_ID);
              assertThat(found.eventName()).isEqualTo("Event 803");
              assertThat(found.eventDate()).isEqualTo(EVENT_DATE);
            });
  }

  @Test
  void readsReservedPositionsAndOrdersByDerivedQueries() {
    save(event(EVENT_ID, EventStatus.PUBLISHED));
    saveOrders(CUSTOMER_ID, List.of(eventOrder(EVENT_ORDER_ID, EVENT_ID)));

    assertThat(queryAdapter.existsOrderPosition(EVENT_ID, 3, 7)).isTrue();
    assertThat(queryAdapter.existsOrderPosition(EVENT_ID, 3, 8)).isFalse();
    assertThat(queryAdapter.countOrdersByIds(List.of(EVENT_ORDER_ID))).isEqualTo(1);
    assertThat(queryAdapter.countOrdersByIdsAndCustomerId(List.of(EVENT_ORDER_ID), CUSTOMER_ID))
        .isEqualTo(1);
  }

  private void save(Event event) {
    new TransactionTemplate(primaryTransactionManager).executeWithoutResult(status -> commandAdapter.save(event));
  }

  private void saveOrders(UUID customerId, List<EventOrder> orders) {
    new TransactionTemplate(primaryTransactionManager)
        .executeWithoutResult(status -> commandAdapter.saveOrders(customerId, orders));
  }

  private void insertUser(JdbcTemplate jdbcTemplate, UUID id, String email, String role) {
    jdbcTemplate.update(
        """
        INSERT INTO ticket_transactional.t_users
            (id, email, password_hash, role, enabled, created_at, updated_at)
        VALUES (?, ?, '{noop}secret', ?, true, ?, ?)
        """,
        id,
        email,
        role,
        NOW,
        NOW);
  }

  private static Event event(UUID id, EventStatus status) {
    return Event.builder()
        .id(id)
        .ownerId(OWNER_ID)
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

  private static EventOrder eventOrder(UUID id, UUID eventId) {
    return EventOrder.builder()
        .id(id)
        .eventId(eventId)
        .rowNumber(3)
        .placeNumber(7)
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
