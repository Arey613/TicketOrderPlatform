package com.example.ticketplatform.api.application.port.out;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface ObjectStoragePort {

  /**
   * Uploads {@code data} to {@code key}, replacing any object already stored at that key.
   */
  String upload(String key, byte[] data, String contentType, String cacheControl, Map<String, String> metadata);

  PresignedUpload issuePresignedUploadUrl(
      String key,
      String contentType,
      String cacheControl,
      long contentLength,
      Map<String, String> metadata);

  Optional<ObjectMetadata> metadata(String key);

  void delete(String key);

  record PresignedUpload(
      URI uploadUrl, Map<String, String> requiredHeaders, Instant expiresAt, URI publicUrl) {}

  record ObjectMetadata(Long contentLength, String contentType, Map<String, String> metadata) {}
}
