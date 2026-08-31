package com.example.ticketplatform.api.adapter.in.web;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticket-order-platform.pagination")
@Getter
@Setter
public class PaginationProperties {

  private EndpointPaginationSettings events = new EndpointPaginationSettings();
  private EndpointPaginationSettings publicEvents = new EndpointPaginationSettings();
  private EndpointPaginationSettings orders = new EndpointPaginationSettings();

  @Getter
  @Setter
  public static class EndpointPaginationSettings {

    private int defaultSize;
    private String defaultSort;
    private List<String> allowedSorts = List.of();
  }
}
