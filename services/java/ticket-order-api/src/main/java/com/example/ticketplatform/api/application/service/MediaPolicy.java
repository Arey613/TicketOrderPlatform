package com.example.ticketplatform.api.application.service;

import java.util.List;

public record MediaPolicy(Image image, Video video) {

  public record Image(Long maxSizeBytes, List<String> allowedContentTypes, String cacheControl) {}

  public record Video(Long maxSizeBytes, List<String> allowedContentTypes, String cacheControl) {}
}
