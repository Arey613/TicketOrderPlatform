package com.example.ticketplatform.api.infrastructure.config.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
class S3ClientConfig {

  @Bean
  S3Client s3Client(S3StorageProperties properties) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(properties.region()))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(Boolean.TRUE.equals(properties.pathStyleAccess()))
                    .build());
    if (StringUtils.hasText(properties.endpoint())) {
      builder.endpointOverride(URI.create(properties.endpoint()));
    }
    return builder.build();
  }

  @Bean
  S3Presigner s3Presigner(S3StorageProperties properties) {
    S3Presigner.Builder builder =
        S3Presigner.builder()
            .region(Region.of(properties.region()))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(Boolean.TRUE.equals(properties.pathStyleAccess()))
                    .build());
    if (StringUtils.hasText(properties.resolvedPresignEndpoint())) {
      builder.endpointOverride(URI.create(properties.resolvedPresignEndpoint()));
    }
    return builder.build();
  }
}
