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

  /**
   * The endpoint a presigned URL must be signed against. Server-side S3 calls (bucket-ensure,
   * direct image upload) run inside the API process and can use {@code endpoint} directly (e.g.
   * the Docker-internal LocalStack hostname), but a presigned URL is handed to the browser, which
   * needs a host-reachable endpoint instead. Falls back to {@code endpoint} when not set
   * separately, which is correct whenever both sides can reach the same address (real AWS, or the
   * API running outside Docker against a local LocalStack).
   */
  public String resolvedPresignEndpoint() {
    return presignEndpoint == null || presignEndpoint.isBlank() ? endpoint : presignEndpoint;
  }
}
