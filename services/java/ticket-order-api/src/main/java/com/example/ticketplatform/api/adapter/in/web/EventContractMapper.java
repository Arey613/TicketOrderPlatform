package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.CreateEventCommand;
import com.example.ticketplatform.api.application.port.in.CreateEventOrderCommand;
import com.example.ticketplatform.api.application.port.in.EventDetailsCommand;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.application.port.in.UpdateEventCommand;
import com.example.ticketplatform.api.domain.model.event.BookedPlace;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.generated.contract.model.BookedPlaceResponse;
import com.example.ticketplatform.api.generated.contract.model.CreateEventOrderItem;
import com.example.ticketplatform.api.generated.contract.model.CreateEventRequest;
import com.example.ticketplatform.api.generated.contract.model.CreatedEventOrderResponse;
import com.example.ticketplatform.api.generated.contract.model.CreatedEventOrdersResponse;
import com.example.ticketplatform.api.generated.contract.model.EventDetailsRequest;
import com.example.ticketplatform.api.generated.contract.model.EventDetailsResponse;
import com.example.ticketplatform.api.generated.contract.model.EventListResponse;
import com.example.ticketplatform.api.generated.contract.model.EventResponse;
import com.example.ticketplatform.api.generated.contract.model.MyEventOrderResponse;
import com.example.ticketplatform.api.generated.contract.model.MyEventOrdersResponse;
import com.example.ticketplatform.api.generated.contract.model.PageMetadata;
import com.example.ticketplatform.api.generated.contract.model.UpdateEventRequest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
interface EventContractMapper {

  @Mapping(target = "ownerId", source = "ownerId")
  @Mapping(target = "date", source = "request.date")
  @Mapping(target = "name", source = "request.name")
  @Mapping(target = "place", source = "request.place")
  @Mapping(target = "type", source = "request.type")
  @Mapping(target = "details", source = "request.details")
  CreateEventCommand toCommand(CreateEventRequest request, UUID ownerId);

  UpdateEventCommand toCommand(UpdateEventRequest request);

  @Mapping(target = "rowNumber", source = "row")
  @Mapping(target = "placeNumber", source = "place")
  CreateEventOrderCommand toCommand(CreateEventOrderItem item);

  EventDetailsCommand toCommand(EventDetailsRequest details);

  @Mapping(target = "eventId", source = "id")
  @Mapping(target = "ordersTaken", expression = "java(event.orders().size())")
  @Mapping(target = "takenPlaces", source = "orders", qualifiedByName = "toPublicBookedPlaceResponses")
  EventResponse toResponse(Event event);

  EventDetailsResponse toResponse(EventDetails details);

  @Mapping(target = "row", source = "rowNumber")
  @Mapping(target = "place", source = "placeNumber")
  @Mapping(target = "isMine", ignore = true)
  BookedPlaceResponse toBookedPlaceResponse(BookedPlace place);

  @Mapping(target = "eventOrderId", source = "id")
  @Mapping(target = "row", source = "rowNumber")
  @Mapping(target = "place", source = "placeNumber")
  CreatedEventOrderResponse toCreatedOrderResponse(EventOrder order);

  @Mapping(target = "eventOrderId", source = "id")
  @Mapping(target = "row", source = "rowNumber")
  @Mapping(target = "place", source = "placeNumber")
  MyEventOrderResponse toMyOrderResponse(EventOrder order);

  @Named("toListResponse")
  default EventListResponse toListResponse(PageResult<Event> events) {
    return new EventListResponse()
        .items(events.items().stream().map(this::toResponse).toList())
        .page(toPageMetadata(events.page()));
  }

  @Named("toMyOrdersResponse")
  default MyEventOrdersResponse toMyOrdersResponse(PageResult<EventOrder> orders) {
    return new MyEventOrdersResponse()
        .items(orders.items().stream().map(this::toMyOrderResponse).toList())
        .page(toPageMetadata(orders.page()));
  }

  @Named("toCreatedOrdersResponse")
  default CreatedEventOrdersResponse toCreatedOrdersResponse(List<EventOrder> orders) {
    return new CreatedEventOrdersResponse()
        .orders(orders.stream().map(this::toCreatedOrderResponse).toList());
  }

  @Named("toPageMetadata")
  default PageMetadata toPageMetadata(
      com.example.ticketplatform.api.application.port.in.PageMetadata page) {
    return new PageMetadata()
        .number(page.number())
        .size(page.size())
        .totalElements(page.totalElements())
        .totalPages(page.totalPages())
        .first(page.first())
        .last(page.last());
  }

  @Named("toPublicBookedPlaceResponses")
  default List<BookedPlaceResponse> toPublicBookedPlaceResponses(List<BookedPlace> orders) {
    return orders.stream().map(this::toBookedPlaceResponse).toList();
  }

  default Instant toInstant(OffsetDateTime dateTime) {
    return dateTime == null ? null : dateTime.toInstant();
  }

  default OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
  }
}
