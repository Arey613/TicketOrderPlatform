package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.AttachEventImageCommand;
import com.example.ticketplatform.api.application.port.in.EventCommandUseCase;
import com.example.ticketplatform.api.application.port.in.EventImageUseCase;
import com.example.ticketplatform.api.application.port.in.EventQueryUseCase;
import com.example.ticketplatform.api.application.port.in.EventVideoUseCase;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.VideoUploadIssuance;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.api.EventsApi;
import com.example.ticketplatform.api.generated.contract.api.PublicApi;
import com.example.ticketplatform.api.generated.contract.model.ConfirmVideoUploadRequest;
import com.example.ticketplatform.api.generated.contract.model.CreateEventOrdersRequest;
import com.example.ticketplatform.api.generated.contract.model.CreateEventRequest;
import com.example.ticketplatform.api.generated.contract.model.CreatedEventOrdersResponse;
import com.example.ticketplatform.api.generated.contract.model.DeleteEventOrdersRequest;
import com.example.ticketplatform.api.generated.contract.model.EventListResponse;
import com.example.ticketplatform.api.generated.contract.model.EventListScope;
import com.example.ticketplatform.api.generated.contract.model.EventResponse;
import com.example.ticketplatform.api.generated.contract.model.IssueVideoUploadUrlRequest;
import com.example.ticketplatform.api.generated.contract.model.IssueVideoUploadUrlResponse;
import com.example.ticketplatform.api.generated.contract.model.PatchEventRequest;
import com.example.ticketplatform.api.generated.contract.model.UpdateEventRequest;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
class EventController implements EventsApi, PublicApi {

  private final EventCommandUseCase eventCommandUseCase;
  private final EventQueryUseCase eventQueryUseCase;
  private final EventImageUseCase eventImageUseCase;
  private final EventVideoUseCase eventVideoUseCase;
  private final CurrentUserProvider currentUserProvider;
  private final EventContractMapper eventContractMapper;
  private final EventResponseAssembler eventResponseAssembler;
  private final EventOrderRequestValidator eventOrderRequestValidator;
  private final PaginationRequestFactory paginationRequestFactory;

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventResponse> attachEventImage(UUID eventId, MultipartFile image) {
    User user = currentUserProvider.currentUser();
    Event event =
        eventImageUseCase.attachEventImage(
            eventId, user.id(), toAttachEventImageCommand(image));
    return ResponseEntity.ok(eventResponseAssembler.toEventResponse(event));
  }

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
  @PreAuthorize("hasRole('CUSTOMER')")
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
  public ResponseEntity<EventResponse> getPublishedEvent(UUID eventId) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventResponse(eventQueryUseCase.getEvent(eventId, null)));
  }

  @Override
  @PreAuthorize("#scope == null || #scope.name() != 'MINE' || hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventListResponse> listEvents(
      EventListScope scope,
      Integer page,
      Integer size,
      String sort) {
    User user = currentUserProvider.currentUser();
    PageRequest pageRequest =
        paginationRequestFactory.eventPage(
            page, size, sort, paginationRequestFactory.authenticatedEventSorts());
    if (scope == EventListScope.MINE) {
      return ResponseEntity.ok(
          eventResponseAssembler.toEventListResponse(eventQueryUseCase.listOwnerEvents(user.id(), pageRequest)));
    }
    return ResponseEntity.ok(
        eventResponseAssembler.toEventListResponse(eventQueryUseCase.listPublishedEvents(pageRequest)));
  }

  @Override
  public ResponseEntity<EventListResponse> listPublishedEvents(
      Integer page,
      Integer size,
      String sort) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventListResponse(
            eventQueryUseCase.listPublishedEvents(
                paginationRequestFactory.publicEventPage(page, size, sort))));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<IssueVideoUploadUrlResponse> issueEventVideoUploadUrl(
      UUID eventId, IssueVideoUploadUrlRequest issueVideoUploadUrlRequest) {
    User user = currentUserProvider.currentUser();
    VideoUploadIssuance issuance =
        eventVideoUseCase.issueVideoUploadUrl(
            eventId, user.id(), eventContractMapper.toCommand(issueVideoUploadUrlRequest));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new IssueVideoUploadUrlResponse(
                eventResponseAssembler.toEventResponse(issuance.event()),
                URI.create(issuance.videoUrl()),
                issuance.uploadUrl(),
                issuance.requiredHeaders(),
                eventContractMapper.toOffsetDateTime(issuance.expiresAt())));
  }

  @Override
  @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
  public ResponseEntity<EventResponse> confirmEventVideoUpload(
      UUID eventId, ConfirmVideoUploadRequest confirmVideoUploadRequest) {
    User user = currentUserProvider.currentUser();
    Event event =
        eventVideoUseCase.confirmVideoUpload(
            eventId, user.id(), eventContractMapper.toCommand(confirmVideoUploadRequest));
    return ResponseEntity.ok(eventResponseAssembler.toEventResponse(event));
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
  public ResponseEntity<EventResponse> patchEvent(UUID eventId, PatchEventRequest patchEventRequest) {
    return ResponseEntity.ok(
        eventResponseAssembler.toEventResponse(
            eventCommandUseCase.patchEvent(
                eventId,
                currentUserProvider.currentUser().id(),
                eventContractMapper.toCommand(patchEventRequest))));
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

  private AttachEventImageCommand toAttachEventImageCommand(MultipartFile image) {
    try {
      return new AttachEventImageCommand(
          image.getBytes(), image.getOriginalFilename(), image.getContentType());
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read uploaded image", exception);
    }
  }
}
