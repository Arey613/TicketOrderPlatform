package com.example.ticketplatform.api.application.port.out;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface ObjectStoragePort {

  String upload(String key, byte[] data, String contentType, String cacheControl);

  PresignedUpload issuePresignedUploadUrl(
      String key, String contentType, String cacheControl, Duration ttl);

  record PresignedUpload(
      URI uploadUrl, Map<String, String> requiredHeaders, Instant expiresAt, URI publicUrl) {}
}
