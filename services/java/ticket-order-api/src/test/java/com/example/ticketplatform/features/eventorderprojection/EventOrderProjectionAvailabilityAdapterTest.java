package com.example.ticketplatform.features.eventorderprojection;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.BookedPlace;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class EventOrderProjectionAvailabilityAdapterTest {

  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000001101");
  private static final Instant NOW = Instant.parse("2026-09-02T12:00:00Z");

  private JdbcClient jdbcClient;
  private DataSource dataSource;
  private TestEventQueryRepositoryPort coreRepository;

  @BeforeEach
  void setUp() {
    DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
    driverManagerDataSource.setDriverClassName("org.h2.Driver");
    driverManagerDataSource.setUrl(
        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
    driverManagerDataSource.setUsername("sa");
    driverManagerDataSource.setPassword("");

    dataSource = driverManagerDataSource;
    jdbcClient = JdbcClient.create(dataSource);
    jdbcClient.sql("CREATE SCHEMA ticket_transactional").update();
    jdbcClient.sql("CREATE SCHEMA ticket_features").update();
    jdbcClient
        .sql(
            """
            CREATE TABLE ticket_transactional.t_event (
                event_id UUID PRIMARY KEY,
                owner_id UUID NOT NULL,
                date TIMESTAMP WITH TIME ZONE NOT NULL,
                name VARCHAR(200) NOT NULL,
                place VARCHAR(200) NOT NULL,
                type VARCHAR(100) NOT NULL,
                status VARCHAR(32) NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
            """)
        .update();
    jdbcClient
        .sql(
            """
            CREATE TABLE ticket_transactional.t_event_details (
                event_details_id UUID PRIMARY KEY,
                event_id UUID NOT NULL,
                description TEXT NOT NULL,
                number_of_places INTEGER NOT NULL,
                number_of_rows INTEGER NOT NULL,
                seats_per_row INTEGER NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL
            )
            """)
        .update();
    jdbcClient
        .sql(
            """
            CREATE TABLE ticket_features.t_event_order_projection (
                event_id UUID NOT NULL,
                row_number INTEGER NOT NULL,
                place_number INTEGER NOT NULL,
                projected_at TIMESTAMP WITH TIME ZONE NOT NULL,
                PRIMARY KEY (event_id, row_number, place_number)
            )
            """)
        .update();
    insertEvent();
    coreRepository = new TestEventQueryRepositoryPort();
  }

  @Test
  void readsProjectedPlacesWhenProjectionRowsExist() {
    coreRepository.event = Optional.of(event(List.of(bookedPlace(9, 9))));
    jdbcClient
        .sql(
            """
            INSERT INTO ticket_features.t_event_order_projection
                (event_id, row_number, place_number, projected_at)
            VALUES (:eventId, 1, 2, :projectedAt), (:eventId, 1, 3, :projectedAt)
            """)
        .param("eventId", EVENT_ID)
        .param("projectedAt", NOW)
        .update();

    EventOrderProjectionAvailabilityAdapter adapter =
        new EventOrderProjectionAvailabilityAdapter(dataSource, coreRepository);

    Event event = adapter.findById(EVENT_ID).orElseThrow();

    assertThat(coreRepository.findByIdCalls).isZero();
    assertThat(event.name()).isEqualTo("Projected event");
    assertThat(event.details().numberOfPlaces()).isEqualTo(100);
    assertThat(event.orders()).extracting(BookedPlace::rowNumber).containsExactly(1, 1);
    assertThat(event.orders()).extracting(BookedPlace::placeNumber).containsExactly(2, 3);
  }

  @Test
  void delegatesToCoreWhenProjectionRowsAreMissing() {
    coreRepository.event = Optional.of(event(List.of(bookedPlace(4, 5))));

    EventOrderProjectionAvailabilityAdapter adapter =
        new EventOrderProjectionAvailabilityAdapter(dataSource, coreRepository);

    Event event = adapter.findById(EVENT_ID).orElseThrow();

    assertThat(coreRepository.findByIdCalls).isEqualTo(1);
    assertThat(event.orders()).singleElement().satisfies(place -> assertThat(place.placeNumber()).isEqualTo(5));
  }

  @Test
  void delegatesToCoreWhenProjectionLookupFails() {
    coreRepository.event = Optional.of(event(List.of(bookedPlace(6, 7))));
    jdbcClient.sql("DROP TABLE ticket_features.t_event_order_projection").update();

    EventOrderProjectionAvailabilityAdapter adapter =
        new EventOrderProjectionAvailabilityAdapter(dataSource, coreRepository);

    Event event = adapter.findById(EVENT_ID).orElseThrow();

    assertThat(coreRepository.findByIdCalls).isEqualTo(1);
    assertThat(event.orders()).singleElement().satisfies(place -> assertThat(place.placeNumber()).isEqualTo(7));
  }

  private void insertEvent() {
    jdbcClient
        .sql(
            """
            INSERT INTO ticket_transactional.t_event
                (event_id, owner_id, date, name, place, type, status, created_at, updated_at)
            VALUES
                (:eventId, :ownerId, :date, 'Projected event', 'Main hall', 'MUSIC',
                 'PUBLISHED', :createdAt, :updatedAt)
            """)
        .param("eventId", EVENT_ID)
        .param("ownerId", UUID.fromString("00000000-0000-0000-0000-000000001102"))
        .param("date", NOW)
        .param("createdAt", NOW)
        .param("updatedAt", NOW)
        .update();
    jdbcClient
        .sql(
            """
            INSERT INTO ticket_transactional.t_event_details
                (event_details_id, event_id, description, number_of_places, number_of_rows,
                 seats_per_row, created_at, updated_at)
            VALUES
                (:detailsId, :eventId, 'Projected event details', 100, 10, 10,
                 :createdAt, :updatedAt)
            """)
        .param("detailsId", UUID.fromString("00000000-0000-0000-0000-000000001103"))
        .param("eventId", EVENT_ID)
        .param("createdAt", NOW)
        .param("updatedAt", NOW)
        .update();
  }

  private static Event event(List<BookedPlace> orders) {
    return Event.builder()
        .id(EVENT_ID)
        .ownerId(UUID.fromString("00000000-0000-0000-0000-000000001102"))
        .date(NOW)
        .name("Projected event")
        .place("Main hall")
        .type("MUSIC")
        .status(EventStatus.PUBLISHED)
        .details(
            EventDetails.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000001103"))
                .description("Projected event details")
                .numberOfPlaces(100)
                .numberOfRows(10)
                .seatsPerRow(10)
                .build())
        .orders(orders)
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static BookedPlace bookedPlace(Integer rowNumber, Integer placeNumber) {
    return BookedPlace.builder()
        .eventId(EVENT_ID)
        .rowNumber(rowNumber)
        .placeNumber(placeNumber)
        .reservationDate(NOW)
        .build();
  }

  private static class TestEventQueryRepositoryPort implements EventQueryRepositoryPort {

    private Optional<Event> event = Optional.empty();
    private int findByIdCalls;

    @Override
    public Optional<Event> findById(UUID id) {
      findByIdCalls++;
      return event;
    }

    @Override
    public PageResult<Event> findPublished(PageRequest pageRequest) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<Event> findByOwnerId(UUID ownerId, PageRequest pageRequest) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<EventOrder> findOrdersByIds(Collection<UUID> ids) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PageResult<EventOrder> findOrdersByCustomerId(UUID customerId, PageRequest pageRequest) {
      throw new UnsupportedOperationException();
    }
  }
}
