package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.EventCommandUseCase;
import com.example.ticketplatform.api.application.port.in.EventQueryUseCase;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.api.EventsApi;
import com.example.ticketplatform.api.generated.contract.model.CreateEventOrdersRequest;
import com.example.ticketplatform.api.generated.contract.model.CreateEventRequest;
import com.example.ticketplatform.api.generated.contract.model.CreatedEventOrdersResponse;
import com.example.ticketplatform.api.generated.contract.model.DeleteEventOrdersRequest;
import com.example.ticketplatform.api.generated.contract.model.EventListResponse;
import com.example.ticketplatform.api.generated.contract.model.EventListScope;
import com.example.ticketplatform.api.generated.contract.model.EventResponse;
import com.example.ticketplatform.api.generated.contract.model.MyEventOrdersResponse;
import com.example.ticketplatform.api.generated.contract.model.UpdateEventRequest;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventController implements EventsApi {

  private final EventCommandUseCase eventCommandUseCase;
  private final EventQueryUseCase eventQueryUseCase;
  private final CurrentUserProvider currentUserProvider;
  private final EventContractMapper eventContractMapper;
  private final EventResponseAssembler eventResponseAssembler;
  private final EventOrderRequestValidator eventOrderRequestValidator;

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventResponse> createEvent(CreateEventRequest createEventRequest) {
    User user = currentUserProvider.currentUser();
    Event event = eventCommandUseCase.createEvent(eventContractMapper.toCommand(createEventRequest, user.id()));
    return ResponseEntity.created(URI.create("/events/" + event.id()))
        .body(eventResponseAssembler.toEventResponse(event));
  }

  @Override
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<CreatedEventOrdersResponse> createEventOrders(
      CreateEventOrdersRequest createEventOrdersRequest) {
    User user = currentUserProvider.currentUser();
    eventOrderRequestValidator.validate(user.id(), createEventOrdersRequest);
    return ResponseEntity.created(URI.create("/events/orders"))
        .body(
            eventContractMapper.toCreatedOrdersResponse(
                eventCommandUseCase.createEventOrders(
                    user.id(),
                    createEventOrdersRequest.getOrders().stream()
                        .map(eventContractMapper::toCommand)
                        .toList())));
  }

  @Override
  public ResponseEntity<Void> deleteEventOrders(DeleteEventOrdersRequest deleteEventOrdersRequest) {
    eventCommandUseCase.deleteEventOrders(
        currentUserProvider.currentUser().id(), deleteEventOrdersRequest.getEventOrderIds());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<EventResponse> getEvent(UUID eventId) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventResponse(
            eventQueryUseCase.getEvent(
                eventId, eventResponseAssembler.currentViewer().map(User::id).orElse(null))));
  }

  @Override
  @PreAuthorize("#scope == null || #scope.name() != 'MINE' || hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventListResponse> listEvents(EventListScope scope) {
    User user = currentUserProvider.currentUser();
    if (scope == EventListScope.MINE) {
      return ResponseEntity.ok(
          eventResponseAssembler.toEventListResponse(eventQueryUseCase.listOwnerEvents(user.id())));
    }
    return ResponseEntity.ok(eventResponseAssembler.toEventListResponse(eventQueryUseCase.listPublishedEvents()));
  }

  @Override
  public ResponseEntity<MyEventOrdersResponse> listMyEventOrders() {
    return ResponseEntity.ok(
        eventContractMapper.toMyOrdersResponse(
            eventQueryUseCase.listUserOrders(currentUserProvider.currentUser().id())));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventResponse> publishEvent(UUID eventId) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventResponse(
            eventCommandUseCase.markEventAsPublished(eventId, currentUserProvider.currentUser().id())));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventResponse> unpublishEvent(UUID eventId) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventResponse(
            eventCommandUseCase.markEventAsDraft(eventId, currentUserProvider.currentUser().id())));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventResponse> updateEvent(UUID eventId, UpdateEventRequest updateEventRequest) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventResponse(
            eventCommandUseCase.updateEvent(
                eventId,
                currentUserProvider.currentUser().id(),
                eventContractMapper.toCommand(updateEventRequest))));
  }
}
