package com.example.ticketplatform.api.infrastructure.config.media;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ticket-order-platform.media")
public record MediaProperties(@DefaultValue Image image, @DefaultValue Video video) {

  public MediaProperties {
    if (image == null) {
      image = defaultImage();
    }
    if (video == null) {
      video = defaultVideo();
    }
  }

  private static Image defaultImage() {
    return new Image(
        5_242_880L, List.of("image/jpeg", "image/png", "image/webp"), "public, max-age=3600");
  }

  private static Video defaultVideo() {
    return new Video(
        List.of("video/mp4", "video/webm", "video/quicktime"), "public, max-age=3600");
  }

  public record Image(
      @DefaultValue("5242880") long maxSizeBytes,
      @DefaultValue({"image/jpeg", "image/png", "image/webp"}) List<String> allowedContentTypes,
      @DefaultValue("public, max-age=3600") String cacheControl) {}

  public record Video(
      @DefaultValue({"video/mp4", "video/webm", "video/quicktime"}) List<String> allowedContentTypes,
      @DefaultValue("public, max-age=3600") String cacheControl) {}
}
