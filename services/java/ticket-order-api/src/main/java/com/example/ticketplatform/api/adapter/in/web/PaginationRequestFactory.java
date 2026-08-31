package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.in.PageRequest;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class PaginationRequestFactory {

  PageRequest eventPage(Integer page, Integer size, String sort, Set<String> allowedSorts) {
    return toPageRequest(page, size, sort, eventDefaults(), allowedSorts);
  }

  PageRequest publicEventPage(Integer page, Integer size, String sort) {
    return toPageRequest(
        page, size, sort, publicEventDefaults(), new HashSet<>(publicEventDefaults().getAllowedSorts()));
  }

  PageRequest orderPage(Integer page, Integer size, String sort) {
    return toPageRequest(page, size, sort, orderDefaults(), new HashSet<>(orderDefaults().getAllowedSorts()));
  }

  Set<String> authenticatedEventSorts() {
    return new HashSet<>(eventDefaults().getAllowedSorts());
  }

  private final PaginationProperties properties;

  PaginationRequestFactory(PaginationProperties properties) {
    this.properties = properties;
  }

  private PageRequest toPageRequest(
      Integer page,
      Integer size,
      String sort,
      PaginationProperties.EndpointPaginationSettings defaults,
      Set<String> allowedSorts) {
    String effectiveSort =
        sort == null ? defaults.getDefaultSort() : normalizeSort(sort, defaults.getDefaultSort());
    if (!allowedSorts.contains(effectiveSort)) {
      throw new IllegalArgumentException("Unsupported sort: " + sort);
    }
    return new PageRequest(page == null ? 0 : page, size == null ? defaults.getDefaultSize() : size, effectiveSort);
  }

  private String normalizeSort(String sort, String defaultSort) {
    if (sort.contains(",")) {
      return sort;
    }
    return sort + defaultSort.substring(defaultSort.indexOf(','));
  }

  private PaginationProperties.EndpointPaginationSettings eventDefaults() {
    return properties.getEvents();
  }

  private PaginationProperties.EndpointPaginationSettings orderDefaults() {
    return properties.getOrders();
  }

  private PaginationProperties.EndpointPaginationSettings publicEventDefaults() {
    return properties.getPublicEvents();
  }
}
