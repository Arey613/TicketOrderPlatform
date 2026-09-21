package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.EventQueryUseCase;
import com.example.ticketplatform.api.generated.contract.api.OrdersApi;
import com.example.ticketplatform.api.generated.contract.model.MyOrdersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class OrdersController implements OrdersApi {

  private final EventQueryUseCase eventQueryUseCase;
  private final CurrentUserProvider currentUserProvider;
  private final EventContractMapper eventContractMapper;
  private final PaginationRequestFactory paginationRequestFactory;

  @Override
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<MyOrdersResponse> listMyOrders(Integer page, Integer size, String sort) {
    return ResponseEntity.ok(
        eventContractMapper.toMyOrdersResponse(
            eventQueryUseCase.listMyOrders(
                currentUserProvider.currentUser().id(),
                paginationRequestFactory.orderPage(page, size, sort))));
  }
}
