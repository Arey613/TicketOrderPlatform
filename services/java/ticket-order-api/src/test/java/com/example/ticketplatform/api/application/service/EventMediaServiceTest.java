package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.application.port.in.AttachEventImageCommand;
import com.example.ticketplatform.api.application.port.in.ConfirmVideoUploadCommand;
import com.example.ticketplatform.api.application.port.in.IssueVideoUploadUrlCommand;
import com.example.ticketplatform.api.application.port.in.VideoUploadIssuance;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.application.port.out.ObjectMetadata;
import com.example.ticketplatform.api.application.port.out.ObjectStoragePort;
import com.example.ticketplatform.api.application.port.out.PresignedUpload;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventOrder;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.example.ticketplatform.api.infrastructure.config.media.MediaProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class EventMediaServiceTest {

  private static final Instant TEST_TIME = Instant.parse("2026-08-11T09:00:00Z");
  private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID OTHER_OWNER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000102");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");
  private static final String VIDEO_SHA256 =
      "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

  @Test
  void attachesImageAndPersistsUploadedUrl() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
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
    assertThat(storage.deletedKeys).isEmpty();
  }

  @Test
  void rejectsImageAttachForNonOwner() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
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
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
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
  void rejectsImageAttachForPublishedEvent() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.PUBLISHED));
    TestObjectStoragePort storage = new TestObjectStoragePort();
    EventMediaService service = newService(events, storage);

    assertThatThrownBy(
            () ->
                service.attachEventImage(
                    EVENT_ID,
                    OWNER_ID,
                    new AttachEventImageCommand(validPngBytes(), "photo.png", "image/png")))
        .isInstanceOf(IllegalStateException.class);
    assertThat(events.savedEvents).isEmpty();
    assertThat(storage.lastUploadKey).isNull();
  }

  @Test
  void deletesUploadedImageWhenSaveFails() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    TestObjectStoragePort storage = new TestObjectStoragePort();
    events.failNextSave = true;
    EventMediaService service = newService(events, storage);

    assertThatThrownBy(
            () ->
                service.attachEventImage(
                    EVENT_ID,
                    OWNER_ID,
                    new AttachEventImageCommand(validPngBytes(), "photo.png", "image/png")))
        .isInstanceOf(IllegalStateException.class);

    assertThat(storage.deletedKeys).containsExactly(storage.lastUploadKey);
  }

  @Test
  void issuesVideoUploadUrlWithoutPersistingItYet() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    TestObjectStoragePort storage = new TestObjectStoragePort();
    EventMediaService service = newService(events, storage);

    VideoUploadIssuance issuance =
        service.issueVideoUploadUrl(
            EVENT_ID,
            OWNER_ID,
            new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4", 1_000L, VIDEO_SHA256));

    assertThat(issuance.uploadUrl()).isEqualTo(storage.lastPresignedUploadUrl);
    assertThat(issuance.videoUrl()).isEqualTo(storage.lastPresignedPublicUrl.toString());
    assertThat(storage.lastPresignKey).startsWith("events/" + EVENT_ID + "/video/");
    assertThat(storage.lastPresignKey).endsWith(".mp4");
    assertThat(storage.lastPresignContentType).isEqualTo("video/mp4");
    assertThat(storage.lastPresignContentLength).isEqualTo(1_000L);
    assertThat(storage.lastPresignMetadata).containsEntry("sha256", VIDEO_SHA256);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsVideoUploadUrlForUnsupportedContentType() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.issueVideoUploadUrl(
                    EVENT_ID,
                    OWNER_ID,
                    new IssueVideoUploadUrlCommand("clip.avi", "video/avi", 1_000L, VIDEO_SHA256)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsVideoUploadUrlExceedingMaxSize() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.issueVideoUploadUrl(
                    EVENT_ID,
                    OWNER_ID,
                    new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4", 200_000_000L, VIDEO_SHA256)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsVideoUploadUrlForNonOwner() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.issueVideoUploadUrl(
                    EVENT_ID,
                    OTHER_OWNER_ID,
                    new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4", 1_000L, VIDEO_SHA256)))
        .isInstanceOf(SecurityException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  @Test
  void rejectsVideoUploadUrlForPublishedEvent() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.PUBLISHED));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.issueVideoUploadUrl(
                    EVENT_ID,
                    OWNER_ID,
                    new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4", 1_000L, VIDEO_SHA256)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void confirmsVideoUploadAndPersistsUrl() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    TestObjectStoragePort storage = new TestObjectStoragePort();
    EventMediaService service = newService(events, storage);

    VideoUploadIssuance issuance =
        service.issueVideoUploadUrl(
            EVENT_ID,
            OWNER_ID,
            new IssueVideoUploadUrlCommand("clip.mp4", "video/mp4", 1_000L, VIDEO_SHA256));

    Event updated =
        service.confirmVideoUpload(
            EVENT_ID,
            OWNER_ID,
            new ConfirmVideoUploadCommand(
                URI.create(issuance.videoUrl()), "video/mp4", 1_000L, VIDEO_SHA256));

    assertThat(updated.videoUrl()).isEqualTo(issuance.videoUrl());
    assertThat(events.savedEvents).hasSize(1);
  }

  @Test
  void rejectsVideoUploadConfirmationForUnrelatedUrl() {
    TestEventRepository events = new TestEventRepository();
    events.events.add(event(EVENT_ID, OWNER_ID, EventStatus.DRAFT));
    EventMediaService service = newService(events, new TestObjectStoragePort());

    assertThatThrownBy(
            () ->
                service.confirmVideoUpload(
                    EVENT_ID,
                    OWNER_ID,
                    new ConfirmVideoUploadCommand(
                        URI.create("https://cdn.example.com/events/other-event/video/x.mp4"),
                        "video/mp4",
                        1_000L,
                        VIDEO_SHA256)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(events.savedEvents).isEmpty();
  }

  private EventMediaService newService(TestEventRepository events, ObjectStoragePort storage) {
    MediaProperties mediaProperties = new MediaProperties(null, null);
    TestUserRepository users = new TestUserRepository(OWNER_ID, OTHER_OWNER_ID);
    SingleConnectionDataSource dataSource =
        new SingleConnectionDataSource("jdbc:h2:mem:event-media-service-test", true);
    return new EventMediaService(
        events,
        new EventAccessGuard(events, users),
        new ImageSignatureValidator(mediaProperties),
        storage,
        Mappers.getMapper(EventApplicationMapper.class),
        Clock.fixed(TEST_TIME, ZoneOffset.UTC)::instant,
        mediaProperties,
        new DataSourceTransactionManager(dataSource));
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

  private static Event event(UUID id, UUID ownerId, EventStatus status) {
    return Event.builder()
        .id(id)
        .ownerId(ownerId)
        .date(TEST_TIME)
        .name("Concert")
        .place("Main hall")
        .type("MUSIC")
        .status(status)
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
    private boolean failNextSave;

    @Override
    public Event save(Event event) {
      if (failNextSave) {
        failNextSave = false;
        throw new IllegalStateException("Simulated save failure");
      }
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

  private static class TestUserRepository
      implements com.example.ticketplatform.api.application.port.out.UserCommandRepositoryPort {

    private final List<User> users;

    TestUserRepository(UUID... userIds) {
      this.users =
          List.of(userIds).stream()
              .map(id -> new User(id, id + "@example.com", "{noop}secret", UserRole.MANAGER, true, TEST_TIME, TEST_TIME))
              .toList();
    }

    @Override
    public Optional<User> findById(UUID id) {
      return users.stream().filter(user -> user.id().equals(id)).findFirst();
    }

    @Override
    public User save(User user) {
      return user;
    }
  }

  private static class TestObjectStoragePort implements ObjectStoragePort {

    private String lastUploadKey;
    private String lastContentType;
    private String lastCacheControl;
    private String lastUploadedUrl;
    private String lastPresignKey;
    private String lastPresignContentType;
    private long lastPresignContentLength;
    private Map<String, String> lastPresignMetadata;
    private URI lastPresignedUploadUrl;
    private URI lastPresignedPublicUrl;
    private final Map<String, ObjectMetadata> metadataByKey = new java.util.HashMap<>();
    private final List<String> deletedKeys = new ArrayList<>();

    @Override
    public String upload(
        String key, byte[] data, String contentType, String cacheControl, Map<String, String> metadata) {
      this.lastUploadKey = key;
      this.lastContentType = contentType;
      this.lastCacheControl = cacheControl;
      this.lastUploadedUrl = "https://cdn.example.com/" + key;
      metadataByKey.put(key, new ObjectMetadata((long) data.length, contentType, metadata));
      return lastUploadedUrl;
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
      this.lastPresignedUploadUrl = URI.create("https://bucket.example.com/" + key + "?signature=abc");
      this.lastPresignedPublicUrl = URI.create("https://cdn.example.com/" + key);
      metadataByKey.put(key, new ObjectMetadata(contentLength, contentType, metadata));
      return new PresignedUpload(
          lastPresignedUploadUrl,
          Map.of(
              "Content-Type",
              contentType,
              "Cache-Control",
              cacheControl,
              "x-amz-meta-sha256",
              metadata.get("sha256")),
          TEST_TIME.plusSeconds(900),
          lastPresignedPublicUrl);
    }

    @Override
    public Optional<ObjectMetadata> metadata(String key) {
      return Optional.ofNullable(metadataByKey.get(key));
    }

    @Override
    public void delete(String key) {
      deletedKeys.add(key);
    }
  }
}
