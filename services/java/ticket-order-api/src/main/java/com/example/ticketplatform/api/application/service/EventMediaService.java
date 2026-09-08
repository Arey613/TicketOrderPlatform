package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.application.port.in.AttachEventImageCommand;
import com.example.ticketplatform.api.application.port.in.ConfirmVideoUploadCommand;
import com.example.ticketplatform.api.application.port.in.EventImageUseCase;
import com.example.ticketplatform.api.application.port.in.EventVideoUseCase;
import com.example.ticketplatform.api.application.port.in.IssueVideoUploadUrlCommand;
import com.example.ticketplatform.api.application.port.in.VideoUploadIssuance;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort.PresignedUpload;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.infrastructure.config.media.MediaProperties;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
  private static final String VIDEO_KEY_INFIX = "/video/";
  private static final String IMAGE_KEY_FORMAT = "events/%s/image/%s.%s";
  private static final String VIDEO_KEY_FORMAT = "events/%s/video/%s.%s";

  private final EventCommandRepositoryPort eventCommandRepositoryPort;
  private final EventAccessGuard eventAccessGuard;
  private final ImageSignatureValidator imageSignatureValidator;
  private final ObjectStoragePort objectStoragePort;
  private final EventApplicationMapper eventApplicationMapper;
  private final Supplier<Instant> currentTimeSupplier;
  private final MediaProperties mediaProperties;
  private final PlatformTransactionManager primaryTransactionManager;

  private <T> T inPrimaryTransaction(TransactionCallback<T> action) {
    return new TransactionTemplate(primaryTransactionManager).execute(action);
  }

  @Override
  public Event attachEventImage(UUID eventId, UUID userId, AttachEventImageCommand command) {
    Event event = eventAccessGuard.requireOwnedDraftEvent(eventId, userId);

    ImageSignatureValidator.ValidationResult validation =
        imageSignatureValidator.validate(command.imageData());
    warnOnContentTypeMismatch(eventId, command.declaredContentType(), validation.contentType());

    String key =
        IMAGE_KEY_FORMAT.formatted(
            eventId, UUID.randomUUID(), imageExtension(validation.contentType()));
    String imageUrl =
        objectStoragePort.upload(
            key,
            command.imageData(),
            validation.contentType(),
            mediaProperties.image().cacheControl());

    Instant now = currentTimeSupplier.get();
    try {
      Event updated =
          inPrimaryTransaction(
              status ->
                  eventCommandRepositoryPort.save(
                      eventApplicationMapper.toEventWithImage(event, imageUrl, now)));

      log.info(
          "event.media.image.attached event_id={} content_type={} size_bytes={}",
          eventId,
          validation.contentType(),
          command.imageData().length);

      return updated;
    } catch (RuntimeException exception) {
      deleteBestEffort(key);
      throw exception;
    }
  }

  @Override
  public VideoUploadIssuance issueVideoUploadUrl(
      UUID eventId, UUID userId, IssueVideoUploadUrlCommand command) {
    Event event = eventAccessGuard.requireOwnedDraftEvent(eventId, userId);

    if (!mediaProperties.video().allowedContentTypes().contains(command.contentType())) {
      throw new IllegalArgumentException(
          "Unsupported video content type: " + command.contentType());
    }
    if (command.fileSizeBytes() == null || command.fileSizeBytes() <= 0) {
      throw new IllegalArgumentException("fileSizeBytes must be a positive number");
    }
    if (command.fileSizeBytes() > mediaProperties.video().maxSizeBytes()) {
      throw new IllegalArgumentException(
          "Video exceeds the maximum allowed size of "
              + mediaProperties.video().maxSizeBytes()
              + " bytes");
    }

    String key =
        VIDEO_KEY_FORMAT.formatted(
            eventId, UUID.randomUUID(), videoExtension(command.contentType()));
    PresignedUpload presignedUpload =
        objectStoragePort.issuePresignedUploadUrl(
            key,
            command.contentType(),
            mediaProperties.video().cacheControl(),
            command.fileSizeBytes());

    log.info(
        "event.media.video.upload_url_issued event_id={} content_type={} size_bytes={}",
        eventId,
        command.contentType(),
        command.fileSizeBytes());

    return new VideoUploadIssuance(
        event,
        presignedUpload.publicUrl().toString(),
        presignedUpload.uploadUrl(),
        presignedUpload.requiredHeaders(),
        presignedUpload.expiresAt());
  }

  @Override
  public Event confirmVideoUpload(UUID eventId, UUID userId, ConfirmVideoUploadCommand command) {
    Event event = eventAccessGuard.requireOwnedDraftEvent(eventId, userId);
    requireVideoUrlBelongsToEvent(eventId, command.videoUrl());

    Instant now = currentTimeSupplier.get();
    Event updated =
        inPrimaryTransaction(
            status ->
                eventCommandRepositoryPort.save(
                    eventApplicationMapper.toEventWithVideo(
                        event, command.videoUrl().toString(), now)));

    log.info("event.media.video.upload_confirmed event_id={}", eventId);

    return updated;
  }

  private void requireVideoUrlBelongsToEvent(UUID eventId, URI videoUrl) {
    String path = videoUrl.getPath();
    String expectedSegment = "/events/" + eventId + VIDEO_KEY_INFIX;
    if (path == null || !path.contains(expectedSegment)) {
      throw new IllegalArgumentException(
          "videoUrl does not correspond to a video upload issued for event " + eventId);
    }
  }

  private void deleteBestEffort(String key) {
    try {
      objectStoragePort.delete(key);
    } catch (RuntimeException exception) {
      // TODO: this failure is only logged today, leaving the object orphaned in storage with
      // no other signal. Once the platform has an alerting system, wire this into it so an
      // admin can intervene instead of relying on log scraping.
      log.warn("event.media.image.compensation_delete_failed key={}", key, exception);
    }
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

  private String imageExtension(String contentType) {
    return IMAGE_EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, DEFAULT_EXTENSION);
  }

  private String videoExtension(String contentType) {
    return VIDEO_EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(contentType, DEFAULT_EXTENSION);
  }
}
