package com.example.ticketplatform.api.infrastructure.config.persistence;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ticket-order-platform.jpa")
public record JpaQueryProperties(Map<String, String> queries) {}
