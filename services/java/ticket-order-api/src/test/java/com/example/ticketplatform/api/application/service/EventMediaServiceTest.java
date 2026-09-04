package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.application.port.in.AttachEventImageCommand;
import com.example.ticketplatform.api.application.port.in.IssueVideoUploadUrlCommand;
import com.example.ticketplatform.api.application.port.in.VideoUploadIssuance;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.infrastructure.config.media.MediaProperties;
import com.example.ticketplatform.api.infrastructure.config.storage.S3StorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EventMediaServiceTest {

  private static final Instant TEST_TIME = Instant.parse("2026-08-11T09:00:00Z");
  private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID OTHER_OWNER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000102");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

  @Test
  void attachesImageAndPersistsUploadedUrl() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID));
    TestObjectStoragePort storage = new TestObjectStoragePort();
    EventMediaService service = newService(events, storage);

    Event updated =
        service.attachEventImage(
            EVENT_ID,
            OWNER_ID,
            new AttachEventImageCommand(validPngBytes(), "photo.png", "image/png"));

    assertThat(updated.imageUrl()).isEqualTo(storage.lastUploadedUrl);
    assertThat(storage.lastUploadKey).startsWith("events/" + EVENT_ID + "/image/");
    assertThat(storage.lastUploadKey).endsWith(".png");
    assertThat(storage.lastContentType).isEqualTo("image/png");
    assertThat(storage.lastCacheControl).isEqualTo("public, max-age=3600");
    assertThat(events.savedEvents).hasSize(1);
  }

  @Test
  void rejectsImageAttachForNonOwner() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.attachEventImage(
                    EVENT_ID,
                    OTHER_OWNER_ID,
                    new AttachEventImageCommand(validPngBytes(), "photo.png", "image/png")))
        .isInstanceOf(SecurityException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsImageAttachForInvalidBytes() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.attachEventImage(
                    EVENT_ID,
                    OWNER_ID,
                    new AttachEventImageCommand(
                        "not an image".getBytes(), "photo.txt", "text/plain")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsImageAttachForMissingEvent() {
    TestEventRepository events = new TestEventRepository();
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.attachEventImage(
                    EVENT_ID,
                    OWNER_ID,
                    new AttachEventImageCommand(validPngBytes(), "photo.png", "image/png")))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void issuesVideoUploadUrlAndPersistsUrlImmediately() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID));
    TestObjectStoragePort storage = new TestObjectStoragePort();
    EventMediaService service = newService(events, storage);

    VideoUploadIssuance issuance =
        service.issueVideoUploadUrl(
            EVENT_ID, OWNER_ID, new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4"));

    assertThat(issuance.uploadUrl()).isEqualTo(storage.lastPresignedUploadUrl);
    assertThat(issuance.event().videoUrl()).isEqualTo(storage.lastPresignedPublicUrl.toString());
    assertThat(storage.lastPresignKey).startsWith("events/" + EVENT_ID + "/video/");
    assertThat(storage.lastPresignKey).endsWith(".mp4");
    assertThat(storage.lastPresignContentType).isEqualTo("video/mp4");
    assertThat(events.savedEvents).hasSize(1);
    // Persisted before any "upload complete" step exists - intentional optimistic persistence.
    assertThat(events.savedEvents.get(0).videoUrl()).isNotNull();
  }

  @Test
  void rejectsVideoUploadUrlForUnsupportedContentType() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.issueVideoUploadUrl(
                    EVENT_ID, OWNER_ID, new IssueVideoUploadUrlCommand("clip.avi", "video/avi")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsVideoUploadUrlForNonOwner() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.issueVideoUploadUrl(
                    EVENT_ID,
                    OTHER_OWNER_ID,
                    new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4")))
        .isInstanceOf(SecurityException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  private EventMediaService newService(TestEventRepository events, ObjectStoragePort storage) {
    MediaProperties mediaProperties = new MediaProperties(null, null);
    return new EventMediaService(
        events,
        new ImageSignatureValidator(mediaProperties),
        storage,
        Mappers.getMapper(EventApplicationMapper.class),
        Clock.fixed(TEST_TIME, ZoneOffset.UTC)::instant,
        mediaProperties,
        new S3StorageProperties(
            "test-bucket",
            "us-east-1",
            "",
            "",
            false,
            "https://cdn.example.com",
            Duration.ofMinutes(15)));
  }

  private static byte[] validPngBytes() {
    try {
      BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(image, "png", output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to build test PNG", exception);
    }
  }

  private static Event event(UUID id, UUID ownerId) {
    return Event.builder()
        .id(id)
        .ownerId(ownerId)
        .date(TEST_TIME)
        .name("Concert")
        .place("Main hall")
        .type("MUSIC")
        .status(EventStatus.PUBLISHED)
        .details(
            EventDetails.builder()
                .id(UUID.randomUUID())
                .description("Evening concert")
                .numberOfPlaces(100)
                .numberOfRows(10)
                .seatsPerRow(10)
                .build())
        .orders(List.of())
        .createdAt(TEST_TIME)
        .updatedAt(TEST_TIME)
        .build();
  }

  private static class TestEventRepository implements EventCommandRepositoryPort {

    private final List<Event> events = new ArrayList<>();
    private final List<Event> savedEvents = new ArrayList<>();

    @Override
    public Event save(Event event) {
      savedEvents.add(event);
      events.removeIf(existing -> existing.id().equals(event.id()));
      events.add(event);
      return event;
    }

    @Override
    public Optional<Event> findById(UUID id) {
      return events.stream().filter(event -> event.id().equals(id)).findFirst();
    }

    @Override
    public List<EventOrder> saveOrders(UUID customerId, List<EventOrder> orders) {
      return orders;
    }

    @Override
    public long deleteOrders(Collection<UUID> ids) {
      return ids.size();
    }
  }

  private static class TestObjectStoragePort implements ObjectStoragePort {

    private String lastUploadKey;
    private String lastContentType;
    private String lastCacheControl;
    private String lastUploadedUrl;
    private String lastPresignKey;
    private String lastPresignContentType;
    private URI lastPresignedUploadUrl;
    private URI lastPresignedPublicUrl;

    @Override
    public String upload(String key, byte[] data, String contentType, String cacheControl) {
      this.lastUploadKey = key;
      this.lastContentType = contentType;
      this.lastCacheControl = cacheControl;
      this.lastUploadedUrl = "https://cdn.example.com/" + key;
      return lastUploadedUrl;
    }

    @Override
    public PresignedUpload issuePresignedUploadUrl(
        String key, String contentType, String cacheControl, Duration ttl) {
      this.lastPresignKey = key;
      this.lastPresignContentType = contentType;
      this.lastPresignedUploadUrl = URI.create("https://bucket.example.com/" + key + "?signature=abc");
      this.lastPresignedPublicUrl = URI.create("https://cdn.example.com/" + key);
      return new PresignedUpload(
          lastPresignedUploadUrl,
          Map.of("Content-Type", contentType, "Cache-Control", cacheControl),
          TEST_TIME.plus(ttl),
          lastPresignedPublicUrl);
    }
  }
}
