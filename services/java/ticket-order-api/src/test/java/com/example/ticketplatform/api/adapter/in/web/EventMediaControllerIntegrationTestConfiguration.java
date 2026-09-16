package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.out.ObjectStoragePort;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort.ObjectMetadata;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class EventMediaControllerIntegrationTestConfiguration {

  @Bean
  @Primary
  StubObjectStoragePort stubObjectStoragePort() {
    return new StubObjectStoragePort();
  }

  static class StubObjectStoragePort implements ObjectStoragePort {

    private String lastUploadKey;
    private String lastUploadContentType;
    private String lastUploadCacheControl;
    private String lastPresignKey;
    private String lastPresignContentType;
    private long lastPresignContentLength;
    private Map<String, String> lastPresignMetadata;
    private final Map<String, ObjectMetadata> metadataByKey = new HashMap<>();
    private final List<String> deletedKeys = new ArrayList<>();

    @Override
    public String upload(
        String key, byte[] data, String contentType, String cacheControl, Map<String, String> metadata) {
      this.lastUploadKey = key;
      this.lastUploadContentType = contentType;
      this.lastUploadCacheControl = cacheControl;
      metadataByKey.put(key, new ObjectMetadata((long) data.length, contentType, metadata));
      return "https://cdn.example.com/" + key;
    }

    @Override
    public PresignedUpload issuePresignedUploadUrl(
        String key,
        String contentType,
        String cacheControl,
        long contentLength,
        Map<String, String> metadata) {
      this.lastPresignKey = key;
      this.lastPresignContentType = contentType;
      this.lastPresignContentLength = contentLength;
      this.lastPresignMetadata = metadata;
      metadataByKey.put(key, new ObjectMetadata(contentLength, contentType, metadata));
      URI uploadUrl = URI.create("https://bucket.example.com/" + key + "?signature=stub");
      URI publicUrl = URI.create("https://cdn.example.com/" + key);
      return new PresignedUpload(
          uploadUrl,
          Map.of(
              "Content-Type",
              contentType,
              "Cache-Control",
              cacheControl,
              "x-amz-meta-sha256",
              metadata.get("sha256")),
          Instant.now().plus(Duration.ofMinutes(15)),
          publicUrl);
    }

    @Override
    public Optional<ObjectMetadata> metadata(String key) {
      return Optional.ofNullable(metadataByKey.get(key));
    }

    @Override
    public void delete(String key) {
      deletedKeys.add(key);
    }

    void reset() {
      lastUploadKey = null;
      lastUploadContentType = null;
      lastUploadCacheControl = null;
      lastPresignKey = null;
      lastPresignContentType = null;
      lastPresignContentLength = 0;
      lastPresignMetadata = null;
      metadataByKey.clear();
      deletedKeys.clear();
    }

    String lastUploadKey() {
      return lastUploadKey;
    }

    String lastUploadContentType() {
      return lastUploadContentType;
    }

    String lastPresignKey() {
      return lastPresignKey;
    }

    String lastPresignContentType() {
      return lastPresignContentType;
    }

    long lastPresignContentLength() {
      return lastPresignContentLength;
    }

    Map<String, String> lastPresignMetadata() {
      return lastPresignMetadata;
    }

    List<String> deletedKeys() {
      return deletedKeys;
    }
  }
}
