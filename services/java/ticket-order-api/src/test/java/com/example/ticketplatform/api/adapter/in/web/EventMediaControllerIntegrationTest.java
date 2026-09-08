package com.example.ticketplatform.api.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketplatform.api.adapter.in.web.EventMediaControllerIntegrationTestConfiguration.StubObjectStoragePort;
import com.example.ticketplatform.api.adapter.in.web.WebControllerIntegrationTestConfiguration.TestUsers;
import com.example.ticketplatform.api.application.port.out.EventCommandRepositoryPort;
import com.example.ticketplatform.api.domain.model.event.Event;
import com.example.ticketplatform.api.domain.model.event.EventDetails;
import com.example.ticketplatform.api.domain.model.event.EventStatus;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import({WebControllerIntegrationTestConfiguration.class, EventMediaControllerIntegrationTestConfiguration.class})
class EventMediaControllerIntegrationTest {

  private static final Instant EVENT_TIME = Instant.parse("2026-09-15T19:30:00Z");
  private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");
  private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
  private static final UUID OTHER_MANAGER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000802");
  private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000803");
  private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000804");
  private static final User MANAGER =
      WebControllerIntegrationTestConfiguration.user(
          MANAGER_ID, "media-manager@example.com", "{noop}secret", UserRole.MANAGER, true);
  private static final User OTHER_MANAGER =
      WebControllerIntegrationTestConfiguration.user(
          OTHER_MANAGER_ID, "other-media-manager@example.com", "{noop}secret", UserRole.MANAGER, true);
  private static final User CUSTOMER =
      WebControllerIntegrationTestConfiguration.user(
          CUSTOMER_ID, "media-customer@example.com", "{noop}secret", UserRole.CUSTOMER, true);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private TestUsers testUsers;

  @Autowired
  private EventCommandRepositoryPort eventCommandRepositoryPort;

  @Autowired
  private StubObjectStoragePort stubObjectStoragePort;

  @BeforeEach
  void setUp() {
    testUsers.reset(List.of(MANAGER, OTHER_MANAGER, CUSTOMER));
    stubObjectStoragePort.reset();
    eventCommandRepositoryPort.save(event());
  }

  @Test
  void attachesImageForOwningManager() throws Exception {
    MockMultipartFile image =
        new MockMultipartFile("image", "photo.png", "image/png", validPngBytes());

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.imageUrl").isNotEmpty());

    assertThat(stubObjectStoragePort.lastUploadContentType()).isEqualTo("image/png");
    assertThat(stubObjectStoragePort.lastUploadKey()).startsWith("events/" + EVENT_ID + "/image/");
  }

  @Test
  void attachesImageWhenDeclaredContentTypeIsMisleadingBecauseBytesAreSniffed() throws Exception {
    MockMultipartFile image =
        new MockMultipartFile("image", "photo.png", "text/plain", validPngBytes());

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isOk());

    assertThat(stubObjectStoragePort.lastUploadContentType()).isEqualTo("image/png");
  }

  @Test
  void rejectsImageAttachForCustomerRole() throws Exception {
    MockMultipartFile image =
        new MockMultipartFile("image", "photo.png", "image/png", validPngBytes());

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsImageAttachForNonOwningManager() throws Exception {
    MockMultipartFile image =
        new MockMultipartFile("image", "photo.png", "image/png", validPngBytes());

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(OTHER_MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsSvgImageAttach() throws Exception {
    byte[] svg =
        "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
            .getBytes(StandardCharsets.UTF_8);
    MockMultipartFile image = new MockMultipartFile("image", "photo.svg", "image/svg+xml", svg);

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsCorruptImageBytes() throws Exception {
    byte[] corrupt = "definitely not an image".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", corrupt);

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsOversizedImagePayloadAtTheApplicationSizeLimit() throws Exception {
    byte[] oversized = new byte[6 * 1024 * 1024];
    MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", oversized);

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void issuesVideoUploadUrlWithoutPersistingItYet() throws Exception {
    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-url", EVENT_ID))
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "fileName": "clip.mp4",
                      "contentType": "video/mp4",
                      "fileSizeBytes": 1000
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.event.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.event.videoUrl").doesNotExist())
        .andExpect(jsonPath("$.videoUrl").isNotEmpty())
        .andExpect(jsonPath("$.uploadUrl").isNotEmpty())
        .andExpect(jsonPath("$.requiredHeaders").isNotEmpty())
        .andExpect(jsonPath("$.expiresAt").isNotEmpty());

    assertThat(stubObjectStoragePort.lastPresignContentType()).isEqualTo("video/mp4");
    assertThat(stubObjectStoragePort.lastPresignKey()).startsWith("events/" + EVENT_ID + "/video/");
    assertThat(stubObjectStoragePort.lastPresignContentLength()).isEqualTo(1000L);
  }

  @Test
  void rejectsVideoUploadUrlExceedingMaxSize() throws Exception {
    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-url", EVENT_ID))
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "fileName": "clip.mp4",
                      "contentType": "video/mp4",
                      "fileSizeBytes": 999999999
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsVideoUploadUrlForCustomerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-url", EVENT_ID))
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "fileName": "clip.mp4",
                      "contentType": "video/mp4",
                      "fileSizeBytes": 1000
                    }
                    """))
        .andExpect(status().isForbidden());
  }

  @Test
  void confirmsVideoUploadAndPersistsUrl() throws Exception {
    String videoUrl = issueVideoUploadUrl();

    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-confirmation", EVENT_ID))
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"videoUrl\": \"" + videoUrl + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()))
        .andExpect(jsonPath("$.videoUrl").value(videoUrl));
  }

  @Test
  void rejectsVideoUploadConfirmationForUnrelatedUrl() throws Exception {
    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-confirmation", EVENT_ID))
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"videoUrl\": \"https://cdn.example.com/events/unrelated/video/x.mp4\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsVideoUploadConfirmationForCustomerRole() throws Exception {
    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-confirmation", EVENT_ID))
                .session(authenticatedSession(CUSTOMER.email(), "ROLE_CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"videoUrl\": \"https://cdn.example.com/events/" + EVENT_ID + "/video/x.mp4\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectsImageAttachForPublishedEvent() throws Exception {
    eventCommandRepositoryPort.save(publish(eventCommandRepositoryPort.findById(EVENT_ID).orElseThrow()));
    MockMultipartFile image =
        new MockMultipartFile("image", "photo.png", "image/png", validPngBytes());

    mockMvc
        .perform(withCsrf(multipart("/events/{eventId}/image", EVENT_ID))
                .file(image)
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER")))
        .andExpect(status().isConflict());
  }

  @Test
  void rejectsVideoUploadUrlForPublishedEvent() throws Exception {
    eventCommandRepositoryPort.save(publish(eventCommandRepositoryPort.findById(EVENT_ID).orElseThrow()));

    mockMvc
        .perform(
            withCsrf(post("/events/{eventId}/video-upload-url", EVENT_ID))
                .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "fileName": "clip.mp4",
                      "contentType": "video/mp4",
                      "fileSizeBytes": 1000
                    }
                    """))
        .andExpect(status().isConflict());
  }

  private String issueVideoUploadUrl() throws Exception {
    String response =
        mockMvc
            .perform(
                withCsrf(post("/events/{eventId}/video-upload-url", EVENT_ID))
                    .session(authenticatedSession(MANAGER.email(), "ROLE_MANAGER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "fileName": "clip.mp4",
                          "contentType": "video/mp4",
                          "fileSizeBytes": 1000
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return JsonPath.read(response, "$.videoUrl");
  }

  private static Event publish(Event event) {
    return new Event(
        event.id(),
        event.ownerId(),
        event.date(),
        event.name(),
        event.place(),
        event.type(),
        EventStatus.PUBLISHED,
        event.details(),
        event.orders(),
        event.imageUrl(),
        event.videoUrl(),
        event.createdAt(),
        event.updatedAt());
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

  private static Event event() {
    return Event.builder()
        .id(EVENT_ID)
        .ownerId(MANAGER_ID)
        .date(EVENT_TIME)
        .name("Media concert")
        .place("Main hall")
        .type("MUSIC")
        .status(EventStatus.DRAFT)
        .details(
            EventDetails.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000805"))
                .description("Large show")
                .numberOfPlaces(120)
                .numberOfRows(12)
                .seatsPerRow(10)
                .build())
        .orders(List.of())
        .createdAt(NOW)
        .updatedAt(NOW)
        .build();
  }

  private static MockHttpSession authenticatedSession(String email, String role) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            email, null, List.of(new SimpleGrantedAuthority(role))));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    return session;
  }

  private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request)
      throws Exception {
    Cookie csrfCookie = csrfCookie();
    return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
  }

  private MockMultipartHttpServletRequestBuilder withCsrf(
      MockMultipartHttpServletRequestBuilder request) throws Exception {
    Cookie csrfCookie = csrfCookie();
    request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
    return request;
  }

  private Cookie csrfCookie() throws Exception {
    return mockMvc
        .perform(get("/auth/csrf"))
        .andExpect(status().isNoContent())
        .andReturn()
        .getResponse()
        .getCookie("XSRF-TOKEN");
  }
}
