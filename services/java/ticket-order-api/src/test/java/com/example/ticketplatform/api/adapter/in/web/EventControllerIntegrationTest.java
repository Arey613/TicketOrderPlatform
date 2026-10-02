package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestEvents;
import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.domain.model.event.BookedPlace;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebControllerIntegrationTestConfiguration.class)
class EventControllerIntegrationTest {

  private static final Instant EVENT_TIME = Instant.parse("2026-09-15T19:30:00Z");
  private static final Instant RESERVATION_TIME = Instant.parse("2026-08-11T10:00:00Z");
  private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000602");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000603");
  private static final UUID EVENT_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000604");
  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000606");
  private static final UUID OTHER_MANAGER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000607");
  private static final UUID OTHER_EVENT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000608");
  private static final User MANAGER =
      WebControllerIntegrationTestConfiguration.user(
          MANAGER_ID, "manager@example.com", "{noop}secret", UserRole.MANAGER, true);
  private static final User ADMIN =
      WebControllerIntegrationTestConfiguration.user(
          ADMIN_ID, "admin.events@example.com", "{noop}secret", UserRole.ADMIN, true);
  private static final User OTHER_MANAGER =
      WebControllerIntegrationTestConfiguration.user(
          OTHER_MANAGER_ID, "other.manager@example.com", "{noop}secret", UserRole.MANAGER, true);
  private static final User CUSTOMER =
      WebControllerIntegrationTestConfiguration.user(
          CUSTOMER_ID, "customer.events@example.com", "{noop}secret", UserRole.CUSTOMER, true);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestUsers testUsers;

  @Autowired
  private TestEvents testEvents;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(MANAGER, ADMIN, OTHER_MANAGER, CUSTOMER));
    Event event = event(EventStatus.PUBLISHED, List.of(bookedPlace()));
    testEvents.reset(List.of(event), List.of(order()));
  }

  @Test
  void rejectsEventCreationForCustomerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                post("/events")
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createEventJson())))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsEventForManagerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                post("/events")
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createEventJson())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ownerId").value(MANAGER_ID.toString()))
        .andExpect(jsonPath("$.name").value("Evening concert"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.details.numberOfPlaces").value(120))
        .andExpect(jsonPath("$.ordersTaken").value(0));

    assertThat(testEvents.lastCommandUserId()).isEqualTo(MANAGER_ID);
  }

  @Test
  void listsPublishedEventsWithTakenPlaces() throws Exception {
    mockMvc
        .perform(get("/events").session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.items[0].ordersTaken").value(1))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].row").value(3))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].place").value(7))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].isMine").value(true))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].placeType").doesNotExist())
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.size").value(10))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.page.totalPages").value(1));
  }

  @Test
  void listsPublishedEventsForAnonymousViewerViaPublicEndpoint() throws Exception {
    mockMvc
        .perform(get("/public/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.items[0].ordersTaken").value(1))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].row").value(3))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].place").value(7))
        .andExpect(jsonPath("$.items[0].takenPlaces[0].isMine").doesNotExist())
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.size").value(10))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.page.totalPages").value(1));
  }

  @Test
  void returnsPublishedEventDetailsForAnonymousViewerWithoutOwnershipHints() throws Exception {
    mockMvc
        .perform(get("/events/{eventId}", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.takenPlaces[0].row").value(3))
        .andExpect(jsonPath("$.takenPlaces[0].place").value(7))
        .andExpect(jsonPath("$.takenPlaces[0].isMine").doesNotExist());
  }

  @Test
  void returnsPublishedEventDetailsForAnonymousViewerViaPublicEndpoint() throws Exception {
    mockMvc
        .perform(get("/public/events/{eventId}", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.takenPlaces[0].row").value(3))
        .andExpect(jsonPath("$.takenPlaces[0].place").value(7))
        .andExpect(jsonPath("$.takenPlaces[0].isMine").doesNotExist());
  }

  @Test
  void returnsCustomerOwnedPlaceHintForAuthenticatedCustomerEventDetails() throws Exception {
    mockMvc
        .perform(
            get("/events/{eventId}", EVENT_ID)
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.takenPlaces[0].isMine").value(true));
  }

  @Test
  void omitsOwnershipHintsForManagerEventDetails() throws Exception {
    mockMvc
        .perform(
            get("/events/{eventId}", EVENT_ID)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.takenPlaces[0].isMine").doesNotExist());
  }

  @Test
  void acceptsCaseInsensitiveEventListScope() throws Exception {
    mockMvc
        .perform(
            get("/events")
                .queryParam("scope", "PUBLISHED")
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventId").value(EVENT_ID.toString()));
  }

  @Test
  void rejectsUnsupportedEventSort() throws Exception {
    mockMvc
        .perform(
            get("/events")
                .queryParam("sort", "reservationDate,desc")
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsOwnedEventListScopeForCustomerRole() throws Exception {
    mockMvc
        .perform(
            get("/events")
                .queryParam("scope", "mine")
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsBulkEventOrdersForCustomer() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                post("/events/orders")
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "orders": [
                            {
                              "eventId": "00000000-0000-0000-0000-000000000603",
                              "row": 4,
                              "place": 8,
                              "placeType": "STANDARD"
                            },
                            {
                              "eventId": "00000000-0000-0000-0000-000000000603",
                              "row": 4,
                              "place": 9,
                              "placeType": "STANDARD"
                            }
                          ]
                        }
                        """)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orders").isArray())
        .andExpect(jsonPath("$.orders.length()").value(2))
        .andExpect(jsonPath("$.orders[0].eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.orders[0].row").value(4))
        .andExpect(jsonPath("$.orders[0].place").value(8))
        .andExpect(jsonPath("$.orders[0].placeType").value("STANDARD"));

    assertThat(testEvents.lastCommandUserId()).isEqualTo(CUSTOMER_ID);
    assertThat(testEvents.createdOrderCount()).isEqualTo(2);
  }

  @Test
  void rejectsEventOrdersForAnonymousViewer() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                post("/events/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createEventOrdersJson(4, 8))))
        .andExpect(status().isUnauthorized());

    assertThat(testEvents.createdOrderCount()).isZero();
  }

  @Test
  void rejectsEventOrdersForManagerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                post("/events/orders")
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createEventOrdersJson(4, 8))))
        .andExpect(status().isForbidden());

    assertThat(testEvents.createdOrderCount()).isZero();
  }

  @Test
  void listsCurrentUserOrders() throws Exception {
    mockMvc
        .perform(
            get("/orders/mine")
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventOrderId").value(EVENT_ORDER_ID.toString()))
        .andExpect(jsonPath("$.items[0].eventName").value("Published concert"))
        .andExpect(jsonPath("$.items[0].eventPlace").value("Main hall"))
        .andExpect(jsonPath("$.items[0].row").value(3))
        .andExpect(jsonPath("$.items[0].place").value(7))
        .andExpect(jsonPath("$.items[0].placeType").value("VIP"))
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.page.size").value(20))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.page.totalPages").value(1));

    assertThat(testEvents.lastQueryUserId()).isEqualTo(CUSTOMER_ID);
    assertThat(testEvents.lastOrderPageRequest().sort()).isEqualTo("eventDate,asc");
  }

  @Test
  void rejectsMyOrdersForAnonymousViewer() throws Exception {
    mockMvc.perform(get("/orders/mine")).andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsMyOrdersForManagerRole() throws Exception {
    mockMvc
        .perform(get("/orders/mine").session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsMyOrderCancellationForAnonymousViewer() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/orders/mine")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(deleteEventOrdersJson(EVENT_ORDER_ID))))
        .andExpect(status().isUnauthorized());

    assertThat(testEvents.deletedOrderCount()).isZero();
  }

  @Test
  void rejectsMyOrderCancellationForManagerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/orders/mine")
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(deleteEventOrdersJson(EVENT_ORDER_ID))))
        .andExpect(status().isForbidden());

    assertThat(testEvents.deletedOrderCount()).isZero();
  }

  @Test
  void cancelsCurrentCustomerOrderUsingAuthenticatedUser() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/orders/mine")
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(deleteEventOrdersJson(EVENT_ORDER_ID))))
        .andExpect(status().isNoContent());

    assertThat(testEvents.lastCommandUserId()).isEqualTo(CUSTOMER_ID);
    assertThat(testEvents.lastDeletedOrderIds()).containsExactly(EVENT_ORDER_ID);
  }

  @Test
  void rejectsMyOrderCancellationWhenUseCaseReportsConflict() throws Exception {
    testEvents.failNextOrderDeletionWithConflict();

    mockMvc
        .perform(
            withCsrf(
                delete("/orders/mine")
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(deleteEventOrdersJson(EVENT_ORDER_ID))))
        .andExpect(status().isConflict());

    assertThat(testEvents.lastCommandUserId()).isEqualTo(CUSTOMER_ID);
    assertThat(testEvents.deletedOrderCount()).isZero();
  }

  @Test
  void ignoresClientCustomerIdentityWhenCancellingCurrentCustomerOrder() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/orders/mine")
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "customerId": "00000000-0000-0000-0000-000000000601",
                          "eventOrderIds": [
                            "00000000-0000-0000-0000-000000000604"
                          ]
                        }
                        """)))
        .andExpect(status().isNoContent());

    assertThat(testEvents.lastCommandUserId()).isEqualTo(CUSTOMER_ID);
    assertThat(testEvents.lastDeletedOrderIds()).containsExactly(EVENT_ORDER_ID);
  }

  @Test
  void removesOldMyEventOrdersRoute() throws Exception {
    mockMvc
        .perform(
            get("/events/orders/mine")
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsEventOrderDeletionForAnonymousViewer() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/events/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "eventOrderIds": [
                            "00000000-0000-0000-0000-000000000604"
                          ]
                        }
                        """)))
        .andExpect(status().isUnauthorized());

    assertThat(testEvents.deletedOrderCount()).isZero();
  }

  @Test
  void rejectsEventOrderDeletionForManagerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/events/orders")
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "eventOrderIds": [
                            "00000000-0000-0000-0000-000000000604"
                          ]
                        }
                        """)))
        .andExpect(status().isForbidden());

    assertThat(testEvents.deletedOrderCount()).isZero();
  }

  @Test
  void returnsPublicEventsListWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/public/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].eventId").value(EVENT_ID.toString()));
  }

  @Test
  void returnsPublicEventDetailsWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/public/events/{eventId}", EVENT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()));
  }

  @Test
  void deletesCurrentUserOrders() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/events/orders")
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "eventOrderIds": [
                            "00000000-0000-0000-0000-000000000604"
                          ]
                        }
                        """)))
        .andExpect(status().isNoContent());

    assertThat(testEvents.lastCommandUserId()).isEqualTo(CUSTOMER_ID);
    assertThat(testEvents.deletedOrderCount()).isEqualTo(1);
  }

  @Test
  void deletesOwnedDraftEventForManagerRole() throws Exception {
    Event draft = event(EventStatus.DRAFT, List.of());
    testEvents.reset(List.of(draft), List.of());

    mockMvc
        .perform(
            withCsrf(
                delete("/events/{eventId}", EVENT_ID)
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))))
        .andExpect(status().isNoContent());

    assertThat(testEvents.lastCommandUserId()).isEqualTo(MANAGER_ID);
  }

  @Test
  void deletesOwnedDraftEventForAdminRole() throws Exception {
    Event draft =
        event(
            OTHER_EVENT_ID,
            ADMIN_ID,
            EventStatus.DRAFT,
            List.of());
    testEvents.reset(List.of(draft), List.of());

    mockMvc
        .perform(
            withCsrf(
                delete("/events/{eventId}", OTHER_EVENT_ID)
                    .session(authenticatedSession(ADMIN.email(), "ROLE_ADMIN"))))
        .andExpect(status().isNoContent());

    assertThat(testEvents.lastCommandUserId()).isEqualTo(ADMIN_ID);
  }

  @Test
  void rejectsEventDeleteForCustomerRole() throws Exception {
    Event draft = event(EventStatus.DRAFT, List.of());
    testEvents.reset(List.of(draft), List.of());

    mockMvc
        .perform(
            withCsrf(
                delete("/events/{eventId}", EVENT_ID)
                    .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsEventDeleteForAnonymousViewer() throws Exception {
    Event draft = event(EventStatus.DRAFT, List.of());
    testEvents.reset(List.of(draft), List.of());

    mockMvc
        .perform(withCsrf(delete("/events/{eventId}", EVENT_ID)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsEventDeleteForNonOwnerManager() throws Exception {
    Event draft = event(EventStatus.DRAFT, List.of());
    testEvents.reset(List.of(draft), List.of());

    mockMvc
        .perform(
            withCsrf(
                delete("/events/{eventId}", EVENT_ID)
                    .session(authenticatedSession(OTHER_MANAGER.email(), "ROLE_MANAGER"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsPublishedEventDelete() throws Exception {
    mockMvc
        .perform(
            withCsrf(
                delete("/events/{eventId}", EVENT_ID)
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsEventDeleteWhenOrdersExist() throws Exception {
    Event draft = event(EventStatus.DRAFT, List.of(bookedPlace()));
    testEvents.reset(List.of(draft), List.of(order()));

    mockMvc
        .perform(
            withCsrf(
                delete("/events/{eventId}", EVENT_ID)
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))))
        .andExpect(status().isConflict());
  }

  private static Event event(EventStatus status, List<BookedPlace> orders) {
    return event(EVENT_ID, MANAGER_ID, status, orders);
  }

  private static Event event(
      UUID eventId,
      UUID ownerId,
      EventStatus status,
      List<BookedPlace> orders) {
    return Event.builder()
        .id(eventId)
        .ownerId(ownerId)
        .date(EVENT_TIME)
        .name("Published concert")
        .place("Main hall")
        .type("MUSIC")
        .status(status)
        .details(
            EventDetails.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000605"))
                .description("Large show")
                .numberOfPlaces(120)
                .numberOfRows(12)
                .seatsPerRow(10)
                .build())
        .orders(orders)
        .createdAt(RESERVATION_TIME)
        .updatedAt(RESERVATION_TIME)
        .build();
  }

  private static BookedPlace bookedPlace() {
    return BookedPlace.builder()
        .id(EVENT_ORDER_ID)
        .eventId(EVENT_ID)
        .customerId(CUSTOMER_ID)
        .rowNumber(3)
        .placeNumber(7)
        .placeType("VIP")
        .reservationDate(RESERVATION_TIME)
        .eventName("Published concert")
        .eventDate(EVENT_TIME)
        .build();
  }

  private static EventOrder order() {
    return EventOrder.builder()
        .id(EVENT_ORDER_ID)
        .eventId(EVENT_ID)
        .customerId(CUSTOMER_ID)
        .rowNumber(3)
        .placeNumber(7)
        .placeType("VIP")
        .reservationDate(RESERVATION_TIME)
        .eventName("Published concert")
        .eventDate(EVENT_TIME)
        .eventPlace("Main hall")
        .build();
  }

  private static MockHttpSession authenticatedSession(String email, String role) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            email, null, List.of(new SimpleGrantedAuthority(role))));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

  private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request)
      throws Exception {
    Cookie csrfCookie = csrfCookie();
    return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
  }

  private Cookie csrfCookie() throws Exception {
    return mockMvc
        .perform(get("/auth/csrf"))
        .andExpect(status().isNoContent())
        .andReturn()
        .getResponse()
        .getCookie("XSRF-TOKEN");
  }

  private static String createEventJson() {
    return """
        {
          "name": "Evening concert",
          "date": "2026-09-15T19:30:00Z",
          "place": "Main hall",
          "type": "MUSIC",
          "details": {
            "description": "Large show",
            "numberOfPlaces": 120,
            "numberOfRows": 12,
            "seatsPerRow": 10
          }
        }
        """;
  }

  private static String createEventOrdersJson(int row, int place) {
    return """
        {
          "orders": [
            {
              "eventId": "00000000-0000-0000-0000-000000000603",
              "row": %d,
              "place": %d,
              "placeType": "STANDARD"
            }
          ]
        }
        """
        .formatted(row, place);
  }

  private static String deleteEventOrdersJson(UUID eventOrderId) {
    return """
        {
          "eventOrderIds": [
            "%s"
          ]
        }
        """
        .formatted(eventOrderId);
  }
}
