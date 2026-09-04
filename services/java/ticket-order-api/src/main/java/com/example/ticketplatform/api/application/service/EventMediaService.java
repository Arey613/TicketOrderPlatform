package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.AttachEventImageCommand;
import com.example.ticketplatform.api.application.port.in.EventImageUseCase;
import com.example.ticketplatform.api.application.port.in.EventVideoUseCase;
import com.example.ticketplatform.api.application.port.in.IssueVideoUploadUrlCommand;
import com.example.ticketplatform.api.application.port.in.VideoUploadIssuance;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort.PresignedUpload;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.infrastructure.config.media.MediaProperties;
import com.example.ticketplatform.api.infrastructure.config.storage.S3StorageProperties;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
class EventMediaService implements EventImageUseCase, EventVideoUseCase {

  private static final Map<String, String> IMAGE_EXTENSIONS_BY_CONTENT_TYPE =
      Map.of(
          "image/jpeg", "jpg",
          "image/png", "png",
          "image/webp", "webp");

  private static final Map<String, String> VIDEO_EXTENSIONS_BY_CONTENT_TYPE =
      Map.of(
          "video/mp4", "mp4",
          "video/webm", "webm",
          "video/quicktime", "mov");

  private static final String DEFAULT_EXTENSION = "bin";

  private final EventCommandRepositoryPort eventCommandRepositoryPort;
  private final ImageSignatureValidator imageSignatureValidator;
  private final ObjectStoragePort objectStoragePort;
  private final EventApplicationMapper eventApplicationMapper;
  private final Supplier<Instant> currentTimeSupplier;
  private final MediaProperties mediaProperties;
  private final S3StorageProperties s3StorageProperties;

  @Override
  @Transactional
  public Event attachEventImage(UUID eventId, UUID userId, AttachEventImageCommand command) {
    Event event = getOwnedEvent(eventId, userId);

    ImageSignatureValidator.ValidationResult validation =
        imageSignatureValidator.validate(command.imageData());
    warnOnContentTypeMismatch(eventId, command.declaredContentType(), validation.contentType());

    String key =
        "events/%s/image/%s.%s"
            .formatted(eventId, UUID.randomUUID(), imageExtension(validation.contentType()));
    String imageUrl =
        objectStoragePort.upload(
            key,
            command.imageData(),
            validation.contentType(),
            mediaProperties.image().cacheControl());

    Instant now = currentTimeSupplier.get();
    Event updated =
        eventCommandRepositoryPort.save(
            eventApplicationMapper.toEventWithImage(event, imageUrl, now));

    log.info(
        "event.media.image.attached event_id={} content_type={} size_bytes={}",
        eventId,
        validation.contentType(),
        command.imageData().length);

    return updated;
  }

  @Override
  @Transactional
  public VideoUploadIssuance issueVideoUploadUrl(
      UUID eventId, UUID userId, IssueVideoUploadUrlCommand command) {
    Event event = getOwnedEvent(eventId, userId);

    if (!mediaProperties.video().allowedContentTypes().contains(command.contentType())) {
      throw new IllegalArgumentException(
          "Unsupported video content type: " + command.contentType());
    }

    String key =
        "events/%s/video/%s.%s"
            .formatted(eventId, UUID.randomUUID(), videoExtension(command.contentType()));
    PresignedUpload presignedUpload =
        objectStoragePort.issuePresignedUploadUrl(
            key,
            command.contentType(),
            mediaProperties.video().cacheControl(),
            s3StorageProperties.presignTtl());

    Instant now = currentTimeSupplier.get();
    Event updated =
        eventCommandRepositoryPort.save(
            eventApplicationMapper.toEventWithVideo(
                event, presignedUpload.publicUrl().toString(), now));

    log.info(
        "event.media.video.upload_url_issued event_id={} content_type={}",
        eventId,
        command.contentType());

    return new VideoUploadIssuance(
        updated,
        presignedUpload.uploadUrl(),
        presignedUpload.requiredHeaders(),
        presignedUpload.expiresAt());
  }

  private void warnOnContentTypeMismatch(UUID eventId, String declaredContentType, String sniffedContentType) {
    if (declaredContentType != null && !Objects.equals(declaredContentType, sniffedContentType)) {
      log.warn(
          "event.media.image.content_type_mismatch event_id={} declared={} sniffed={}",
          eventId,
          declaredContentType,
          sniffedContentType);
    }
  }

  private Event getOwnedEvent(UUID eventId, UUID userId) {
    Event event =
        eventCommandRepositoryPort
            .findById(eventId)
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId));
    if (!event.ownerId().equals(userId)) {
      throw new SecurityException("User does not own event: " + eventId);
    }
    return event;
  }

  private String imageExtension(String contentType) {
    return IMAGE_EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, DEFAULT_EXTENSION);
  }

  private String videoExtension(String contentType) {
    return VIDEO_EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, DEFAULT_EXTENSION);
  }
}
