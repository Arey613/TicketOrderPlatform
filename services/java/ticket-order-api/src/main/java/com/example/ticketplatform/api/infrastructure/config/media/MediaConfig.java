package com.example.ticketplatform.api.infrastructure.config.media;

import com.example.ticketplatform.api.application.service.MediaPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MediaProperties.class)
class MediaConfig {

  @Bean
  MediaPolicy mediaPolicy(MediaProperties mediaProperties) {
    return new MediaPolicy(
        new MediaPolicy.Image(
            mediaProperties.image().maxSizeBytes(),
            mediaProperties.image().allowedContentTypes(),
            mediaProperties.image().cacheControl()),
        new MediaPolicy.Video(
            mediaProperties.video().maxSizeBytes(),
            mediaProperties.video().allowedContentTypes(),
            mediaProperties.video().cacheControl()));
  }
}
