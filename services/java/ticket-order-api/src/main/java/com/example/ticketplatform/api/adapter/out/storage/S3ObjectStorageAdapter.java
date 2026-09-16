package com.example.ticketplatform.api.adapter.out.storage;

import com.example.ticketplatform.api.application.port.out.ObjectStoragePort;
import com.example.ticketplatform.api.infrastructure.config.storage.S3StorageProperties;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Repository
@RequiredArgsConstructor
@Slf4j
class S3ObjectStorageAdapter implements ObjectStoragePort {

  private static final int NOT_FOUND_STATUS_CODE = 404;

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3StorageProperties s3StorageProperties;
  private final Supplier<Instant> currentTimeSupplier;
  private volatile boolean bucketEnsured = false;
  private volatile String normalizedPublicBaseUrl;

  private String normalizedPublicBaseUrl() {
    String cached = normalizedPublicBaseUrl;
    if (cached != null) {
      return cached;
    }
    String baseUrl = s3StorageProperties.publicBaseUrl();
    String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    normalizedPublicBaseUrl = normalized;
    return normalized;
  }

  private void ensureBucketExists() {
    if (bucketEnsured) {
      return;
    }
    synchronized (this) {
      if (bucketEnsured) {
        return;
      }
      try {
        s3Client.headBucket(
            HeadBucketRequest.builder().bucket(s3StorageProperties.bucket()).build());
      } catch (NoSuchBucketException exception) {
        createBucket();
      } catch (S3Exception exception) {
        if (exception.statusCode() == NOT_FOUND_STATUS_CODE) {
          createBucket();
        } else {
          throw exception;
        }
      }
      bucketEnsured = true;
    }
  }

  @Override
  public String upload(
      String key, byte[] data, String contentType, String cacheControl, Map<String, String> metadata) {
    ensureBucketExists();
    s3Client.putObject(
        PutObjectRequest.builder()
            .bucket(s3StorageProperties.bucket())
            .key(key)
            .contentType(contentType)
            .cacheControl(cacheControl)
            .metadata(metadata)
            .build(),
        RequestBody.fromBytes(data));
    return buildPublicUrl(key).toString();
  }

  @Override
  public PresignedUpload issuePresignedUploadUrl(
      String key,
      String contentType,
      String cacheControl,
      long contentLength,
      Map<String, String> metadata) {
    ensureBucketExists();
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(s3StorageProperties.bucket())
            .key(key)
            .contentType(contentType)
            .cacheControl(cacheControl)
            .contentLength(contentLength)
            .metadata(metadata)
            .build();

    PresignedPutObjectRequest presignedRequest =
        s3Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(s3StorageProperties.presignTtl())
                .putObjectRequest(putObjectRequest)
                .build());

    return new PresignedUpload(
        toUri(presignedRequest),
        toRequiredHeaders(presignedRequest),
        currentTimeSupplier.get().plus(s3StorageProperties.presignTtl()),
        buildPublicUrl(key));
  }

  @Override
  public Optional<ObjectMetadata> metadata(String key) {
    ensureBucketExists();
    try {
      HeadObjectResponse response =
          s3Client.headObject(
              HeadObjectRequest.builder().bucket(s3StorageProperties.bucket()).key(key).build());
      return Optional.of(
          new ObjectMetadata(response.contentLength(), response.contentType(), response.metadata()));
    } catch (NoSuchKeyException exception) {
      return Optional.empty();
    } catch (S3Exception exception) {
      if (exception.statusCode() == NOT_FOUND_STATUS_CODE) {
        return Optional.empty();
      }
      throw exception;
    }
  }

  @Override
  public void delete(String key) {
    ensureBucketExists();
    s3Client.deleteObject(
        DeleteObjectRequest.builder().bucket(s3StorageProperties.bucket()).key(key).build());
  }

  private URI toUri(PresignedPutObjectRequest presignedRequest) {
    try {
      return presignedRequest.url().toURI();
    } catch (URISyntaxException exception) {
      throw new RuntimeException("Presigned S3 URL is not a valid URI", exception);
    }
  }

  private Map<String, String> toRequiredHeaders(PresignedPutObjectRequest presignedRequest) {
    Map<String, String> headers = new LinkedHashMap<>();
    presignedRequest
        .signedHeaders()
        .forEach((name, values) -> headers.put(name, String.join(",", values)));
    return headers;
  }

  private URI buildPublicUrl(String key) {
    return URI.create(normalizedPublicBaseUrl() + "/" + key);
  }

  private void createBucket() {
    try {
      s3Client.createBucket(
          CreateBucketRequest.builder().bucket(s3StorageProperties.bucket()).build());
      log.info("storage.bucket.created bucket={}", s3StorageProperties.bucket());
    } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException exception) {
      log.info("storage.bucket.already_exists bucket={}", s3StorageProperties.bucket());
    }
  }
}
