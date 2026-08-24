package com.example.ticketplatform.api.adapter.out.persistence.event;

import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface EventMapper {

  @Mapping(target = "status", expression = "java(toEntityStatus(event.status()))")
  @Mapping(target = "details", ignore = true)
  @Mapping(target = "orders", expression = "java(java.util.List.of())")
  EventEntity toEventEntity(Event event);

  @Mapping(target = "id", source = "details.id")
  @Mapping(target = "description", source = "details.description")
  @Mapping(target = "numberOfPlaces", source = "details.numberOfPlaces")
  @Mapping(target = "numberOfRows", source = "details.numberOfRows")
  @Mapping(target = "seatsPerRow", source = "details.seatsPerRow")
  @Mapping(target = "createdAt", source = "event.createdAt")
  @Mapping(target = "updatedAt", source = "event.updatedAt")
  @Mapping(target = "event", source = "event")
  EventDetailsEntity toEventDetailsEntity(EventDetails details, EventEntity event);

  @Mapping(target = "id", source = "order.id")
  @Mapping(target = "customerId", source = "customerId")
  @Mapping(target = "customerReference", source = "order.customerReference")
  @Mapping(target = "rowNumber", source = "order.rowNumber")
  @Mapping(target = "placeNumber", source = "order.placeNumber")
  @Mapping(target = "placeType", source = "order.placeType")
  @Mapping(target = "reservationDate", source = "order.reservationDate")
  @Mapping(target = "event", source = "event")
  EventOrderEntity toEntity(EventOrder order, EventEntity event, java.util.UUID customerId);

  @Mapping(target = "status", expression = "java(toDomainStatus(entity.getStatus()))")
  @Mapping(target = "orders", expression = "java(toDomainOrders(entity.getOrders()))")
  Event toDomain(EventEntity entity);

  EventDetails toDomain(EventDetailsEntity entity);

  @Mapping(target = "eventId", source = "event.id")
  @Mapping(target = "eventName", source = "event.name")
  @Mapping(target = "eventDate", source = "event.date")
  EventOrder toDomain(EventOrderEntity entity);

  default EventEntity toEntity(Event event) {
    EventEntity entity = toEventEntity(event);
    entity.attachDetails(toEventDetailsEntity(event.details(), entity));
    return entity;
  }

  default List<EventOrder> toDomainOrders(List<EventOrderEntity> entities) {
    return entities.stream().map(this::toDomain).toList();
  }

  default EventStatusEntity toEntityStatus(EventStatus status) {
    return status == null ? null : EventStatusEntity.valueOf(status.name());
  }

  default EventStatus toDomainStatus(EventStatusEntity status) {
    return status == null ? null : EventStatus.valueOf(status.name());
  }
}
