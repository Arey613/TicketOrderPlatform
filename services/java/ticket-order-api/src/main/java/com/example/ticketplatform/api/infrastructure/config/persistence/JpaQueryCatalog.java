package com.example.ticketplatform.api.infrastructure.config.persistence;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class JpaQueryCatalog {

  private static final String[] REQUIRED_QUERY_KEYS = {
    "com.example.ticketplatform.api.adapter.out.persistence.event.EventQueryPersistenceAdapter.findPublished",
    "com.example.ticketplatform.api.adapter.out.persistence.event.EventQueryPersistenceAdapter.findByOwnerId",
    "com.example.ticketplatform.api.adapter.out.persistence.event.EventQueryPersistenceAdapter.findOrdersByCustomerId",
    "com.example.ticketplatform.api.adapter.out.persistence.user.UserQueryPersistenceAdapter.findByEmail"
  };

  private final Map<String, String> queries;

  JpaQueryCatalog(JpaQueryProperties properties) {
    this.queries = properties.queries() == null ? Map.of() : Map.copyOf(properties.queries());
    for (String key : REQUIRED_QUERY_KEYS) {
      get(key);
    }
  }

  public String get(Class<?> owner, String methodName) {
    return get(owner.getName() + "." + methodName);
  }

  public String get(String key) {
    String query = queries.get(key);
    Assert.hasText(query, () -> "Missing JPA query for key '%s'".formatted(key));
    return query;
  }

  public static String[] requiredQueryKeys() {
    return REQUIRED_QUERY_KEYS.clone();
  }
}
