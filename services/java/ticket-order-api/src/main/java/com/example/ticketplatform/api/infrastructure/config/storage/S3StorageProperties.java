package com.example.ticketplatform.api.infrastructure.config.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ticket-order-platform.storage.s3")
public record S3StorageProperties(
    String bucket,
    @DefaultValue("us-east-1") String region,
    @DefaultValue("") String endpoint,
    @DefaultValue("") String presignEndpoint,
    @DefaultValue("false") Boolean pathStyleAccess,
    String publicBaseUrl,
    @DefaultValue("15m") Duration presignTtl) {

  public String resolvedPresignEndpoint() {
    return presignEndpoint == null || presignEndpoint.isBlank() ? endpoint : presignEndpoint;
  }
}
