package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.CreateEventCommand;
import com.example.ticketplatform.api.application.port.in.CreateEventOrderCommand;
import com.example.ticketplatform.api.application.port.in.EventDetailsCommand;
import com.example.ticketplatform.api.application.port.in.UpdateEventCommand;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface EventApplicationMapper {

  @Mapping(target = "id", source = "eventId")
  @Mapping(target = "ownerId", source = "command.ownerId")
  @Mapping(target = "status", source = "status")
  @Mapping(target = "date", source = "command.date")
  @Mapping(target = "name", source = "command.name")
  @Mapping(target = "place", source = "command.place")
  @Mapping(target = "type", source = "command.type")
  @Mapping(target = "details", source = "details")
  @Mapping(target = "orders", source = "orders")
  @Mapping(target = "createdAt", source = "now")
  @Mapping(target = "updatedAt", source = "now")
  Event toEvent(
      CreateEventCommand command,
      UUID eventId,
      EventDetails details,
      EventStatus status,
      List<EventOrder> orders,
      Instant now);

  @Mapping(target = "id", source = "existing.id")
  @Mapping(target = "ownerId", source = "existing.ownerId")
  @Mapping(target = "date", source = "command.date")
  @Mapping(target = "name", source = "command.name")
  @Mapping(target = "place", source = "command.place")
  @Mapping(target = "type", source = "command.type")
  @Mapping(target = "status", source = "existing.status")
  @Mapping(target = "details", source = "details")
  @Mapping(target = "orders", source = "existing.orders")
  @Mapping(target = "createdAt", source = "existing.createdAt")
  @Mapping(target = "updatedAt", source = "now")
  Event toUpdatedEvent(Event existing, UpdateEventCommand command, EventDetails details, Instant now);

  @Mapping(target = "id", source = "detailsId")
  EventDetails toDetails(EventDetailsCommand command, UUID detailsId);

  @Mapping(target = "status", source = "status")
  @Mapping(target = "updatedAt", source = "now")
  Event toEventWithStatus(Event event, EventStatus status, Instant now);

  @Mapping(target = "id", source = "eventOrderId")
  @Mapping(target = "customerReference", source = "customerReference")
  @Mapping(target = "rowNumber", source = "command.rowNumber")
  @Mapping(target = "placeNumber", source = "command.placeNumber")
  @Mapping(target = "reservationDate", source = "now")
  @Mapping(target = "eventName", ignore = true)
  @Mapping(target = "eventDate", ignore = true)
  EventOrder toOrder(
      CreateEventOrderCommand command, UUID eventOrderId, UUID customerReference, Instant now);
}
