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
  private static final User MANAGER =
      WebControllerIntegrationTestConfiguration.user(
          MANAGER_ID, "manager@example.com", "{noop}secret", UserRole.MANAGER, true);
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
    testUsers.reset(List.of(MANAGER, CUSTOMER));
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
        .andExpect(jsonPath("$.events[0].eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.events[0].ordersTaken").value(1))
        .andExpect(jsonPath("$.events[0].takenPlaces[0].row").value(3))
        .andExpect(jsonPath("$.events[0].takenPlaces[0].place").value(7))
        .andExpect(jsonPath("$.events[0].takenPlaces[0].isMine").value(true))
        .andExpect(jsonPath("$.events[0].takenPlaces[0].placeType").doesNotExist());
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
        .andExpect(jsonPath("$.events[0].eventId").value(EVENT_ID.toString()));
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
            get("/events/orders/mine")
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orders[0].eventOrderId").value(EVENT_ORDER_ID.toString()))
        .andExpect(jsonPath("$.orders[0].eventName").value("Published concert"))
        .andExpect(jsonPath("$.orders[0].row").value(3))
        .andExpect(jsonPath("$.orders[0].place").value(7))
        .andExpect(jsonPath("$.orders[0].placeType").value("VIP"));
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

  private static Event event(EventStatus status, List<BookedPlace> orders) {
    return Event.builder()
        .id(EVENT_ID)
        .ownerId(MANAGER_ID)
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
}
