package com.example.ticketplatform.features.eventorderprojection;

import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.out.EventQueryRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.BookedPlace;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Slf4j
@Primary
@Repository
class EventOrderProjectionAvailabilityAdapter implements EventQueryRepositoryPort {

  private final JdbcClient jdbcClient;
  private final EventQueryRepositoryPort coreEventQueryRepositoryPort;

  EventOrderProjectionAvailabilityAdapter(
      @Qualifier("primaryDataSource") DataSource primaryDataSource,
      @Qualifier("eventQueryPersistenceAdapter") EventQueryRepositoryPort coreEventQueryRepositoryPort) {
    this.jdbcClient = JdbcClient.create(primaryDataSource);
    this.coreEventQueryRepositoryPort = coreEventQueryRepositoryPort;
  }

  @Override
  public Optional<Event> findById(UUID id) {
    try {
      Optional<Event> event = findEventWithoutOrders(id);
      if (event.isEmpty()) {
        return event;
      }

      List<BookedPlace> projectedPlaces =
          jdbcClient
              .sql(
                  """
                  SELECT event_id, row_number, place_number, projected_at
                  FROM ticket_features.t_event_order_projection
                  WHERE event_id = :eventId
                  ORDER BY row_number, place_number
                  """)
              .param("eventId", id)
              .query(
                  (resultSet, rowNumber) ->
                      BookedPlace.builder()
                          .eventId(resultSet.getObject("event_id", UUID.class))
                          .rowNumber(resultSet.getInt("row_number"))
                          .placeNumber(resultSet.getInt("place_number"))
                          .reservationDate(resultSet.getTimestamp("projected_at").toInstant())
                          .build())
              .list();

      if (projectedPlaces.isEmpty()) {
        log.info("Event order projection miss for event {}", id);
        return coreEventQueryRepositoryPort.findById(id);
      }

      return event.map(found -> withProjectedPlaces(found, projectedPlaces));
    } catch (DataAccessException exception) {
      log.warn("Event order projection lookup failed for event {}", id, exception);
      return coreEventQueryRepositoryPort.findById(id);
    }
  }

  @Override
  public PageResult<Event> findPublished(PageRequest pageRequest) {
    return coreEventQueryRepositoryPort.findPublished(pageRequest);
  }

  @Override
  public PageResult<Event> findByOwnerId(UUID ownerId, PageRequest pageRequest) {
    return coreEventQueryRepositoryPort.findByOwnerId(ownerId, pageRequest);
  }

  @Override
  public boolean existsOrderPosition(UUID eventId, int rowNumber, int placeNumber) {
    return coreEventQueryRepositoryPort.existsOrderPosition(eventId, rowNumber, placeNumber);
  }

  @Override
  public List<EventOrder> findOrdersByIds(Collection<UUID> ids) {
    return coreEventQueryRepositoryPort.findOrdersByIds(ids);
  }

  @Override
  public List<EventOrder> findOrdersByIdsAndCustomerId(Collection<UUID> ids, UUID customerId) {
    return coreEventQueryRepositoryPort.findOrdersByIdsAndCustomerId(ids, customerId);
  }

  @Override
  public PageResult<EventOrder> findOrdersByCustomerId(UUID customerId, PageRequest pageRequest) {
    return coreEventQueryRepositoryPort.findOrdersByCustomerId(customerId, pageRequest);
  }

  private Event withProjectedPlaces(Event event, List<BookedPlace> projectedPlaces) {
    return Event.builder()
        .id(event.id())
        .ownerId(event.ownerId())
        .date(event.date())
        .name(event.name())
        .place(event.place())
        .type(event.type())
        .status(event.status())
        .details(event.details())
        .orders(projectedPlaces)
        .createdAt(event.createdAt())
        .updatedAt(event.updatedAt())
        .build();
  }

  private Optional<Event> findEventWithoutOrders(UUID id) {
    return jdbcClient
        .sql(
            """
            SELECT e.event_id,
                   e.owner_id,
                   e.date,
                   e.name,
                   e.place,
                   e.type,
                   e.status,
                   e.created_at,
                   e.updated_at,
                   d.event_details_id,
                   d.description,
                   d.number_of_places,
                   d.number_of_rows,
                   d.seats_per_row
            FROM ticket_transactional.t_event e
            JOIN ticket_transactional.t_event_details d ON d.event_id = e.event_id
            WHERE e.event_id = :eventId
            """)
        .param("eventId", id)
        .query(this::toEventWithoutOrders)
        .list()
        .stream()
        .findFirst();
  }

  private Event toEventWithoutOrders(ResultSet resultSet, int rowNumber) throws SQLException {
    return Event.builder()
        .id(resultSet.getObject("event_id", UUID.class))
        .ownerId(resultSet.getObject("owner_id", UUID.class))
        .date(toInstant(resultSet.getTimestamp("date")))
        .name(resultSet.getString("name"))
        .place(resultSet.getString("place"))
        .type(resultSet.getString("type"))
        .status(EventStatus.valueOf(resultSet.getString("status")))
        .details(
            EventDetails.builder()
                .id(resultSet.getObject("event_details_id", UUID.class))
                .description(resultSet.getString("description"))
                .numberOfPlaces(resultSet.getInt("number_of_places"))
                .numberOfRows(resultSet.getInt("number_of_rows"))
                .seatsPerRow(resultSet.getInt("seats_per_row"))
                .build())
        .orders(List.of())
        .createdAt(toInstant(resultSet.getTimestamp("created_at")))
        .updatedAt(toInstant(resultSet.getTimestamp("updated_at")))
        .build();
  }

  private Instant toInstant(Timestamp timestamp) {
    return timestamp.toInstant();
  }
}
