package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.domain.model.event.BookedPlace;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.generated.contract.model.BookedPlaceResponse;
import com.example.ticketplatform.api.generated.contract.model.EventListResponse;
import com.example.ticketplatform.api.generated.contract.model.EventResponse;
import com.example.ticketplatform.api.generated.contract.model.PageMetadata;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class EventResponseAssembler {

  private final CurrentUserProvider currentUserProvider;
  private final EventContractMapper eventContractMapper;

  EventResponse toEventResponse(Event event) {
    Optional<User> viewer = currentUserProvider.optionalCurrentUser();
    EventResponse response = eventContractMapper.toResponse(event);
    response.setTakenPlaces(toBookedPlaceResponses(event.orders(), viewer));
    return response;
  }

  EventListResponse toEventListResponse(PageResult<Event> events) {
    return new EventListResponse()
        .items(events.items().stream().map(this::toEventResponse).toList())
        .page(toPageMetadata(events.page()));
  }

  Optional<User> currentViewer() {
    return currentUserProvider.optionalCurrentUser();
  }

  private PageMetadata toPageMetadata(
      com.example.ticketplatform.api.application.port.in.PageMetadata page) {
    return new PageMetadata()
        .number(page.number())
        .size(page.size())
        .totalElements(page.totalElements())
        .totalPages(page.totalPages())
        .first(page.first())
        .last(page.last());
  }

  private List<BookedPlaceResponse> toBookedPlaceResponses(
      List<BookedPlace> orders, Optional<User> viewer) {
    return orders.stream().map(order -> toBookedPlaceResponse(order, viewer)).toList();
  }

  private BookedPlaceResponse toBookedPlaceResponse(BookedPlace order, Optional<User> viewer) {
    BookedPlaceResponse response = eventContractMapper.toBookedPlaceResponse(order);
    viewer.ifPresent(user -> decorateForViewer(response, order, user));
    return response;
  }

  private void decorateForViewer(BookedPlaceResponse response, BookedPlace order, User viewer) {
    if (viewer.role() == UserRole.CUSTOMER) {
      response.setIsMine(viewer.id().equals(order.customerId()));
    }
  }
}
