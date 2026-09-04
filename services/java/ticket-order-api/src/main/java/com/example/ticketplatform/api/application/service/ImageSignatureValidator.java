package com.example.ticketplatform.api.application.service;

import com.example.ticketplatform.api.infrastructure.config.media.MediaProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates event image bytes server-side: size, magic-byte signature against an allow-list of
 * JPEG/PNG/WEBP, and that the bytes actually decode as an image. Never trusts a declared MIME
 * type or file extension.
 */
@Component
@RequiredArgsConstructor
public class ImageSignatureValidator {

  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final String RIFF_ASCII = "RIFF";
  private static final String WEBP_ASCII = "WEBP";
  private static final int WEBP_HEADER_LENGTH = 12;
  private static final int WEBP_FORMAT_OFFSET = 8;

  private final MediaProperties mediaProperties;

  public ValidationResult validate(byte[] imageData) {
    if (imageData == null || imageData.length == 0) {
      throw new IllegalArgumentException("Image data must not be empty");
    }
    long maxSizeBytes = mediaProperties.image().maxSizeBytes();
    if (imageData.length > maxSizeBytes) {
      throw new IllegalArgumentException(
          "Image exceeds the maximum allowed size of " + maxSizeBytes + " bytes");
    }

    String sniffedContentType =
        sniffContentType(imageData)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Image bytes do not match an allowed signature (JPEG, PNG, WEBP)"));

    // The JDK ships no built-in ImageIO WEBP reader, so ImageIO.read() can never confirm a
    // genuine WEBP payload. The RIFF/WEBP container signature check above is the strongest
    // verification available without adding a new imaging dependency; only JPEG/PNG (which the
    // JDK can always decode) go through the additional ImageIO decode check.
    if (!"image/webp".equals(sniffedContentType) && !decodesAsImage(imageData)) {
      throw new IllegalArgumentException("Image bytes could not be decoded as a valid image");
    }

    return new ValidationResult(sniffedContentType);
  }

  private boolean decodesAsImage(byte[] imageData) {
    try {
      return ImageIO.read(new ByteArrayInputStream(imageData)) != null;
    } catch (IOException exception) {
      return false;
    }
  }

  private Optional<String> sniffContentType(byte[] data) {
    if (startsWith(data, JPEG_SIGNATURE)) {
      return Optional.of("image/jpeg");
    }
    if (startsWith(data, PNG_SIGNATURE)) {
      return Optional.of("image/png");
    }
    if (isWebp(data)) {
      return Optional.of("image/webp");
    }
    return Optional.empty();
  }

  private boolean isWebp(byte[] data) {
    if (data.length < WEBP_HEADER_LENGTH) {
      return false;
    }
    return matchesAscii(data, 0, RIFF_ASCII) && matchesAscii(data, WEBP_FORMAT_OFFSET, WEBP_ASCII);
  }

  private boolean startsWith(byte[] data, byte[] signature) {
    if (data.length < signature.length) {
      return false;
    }
    for (int i = 0; i < signature.length; i++) {
      if (data[i] != signature[i]) {
        return false;
      }
    }
    return true;
  }

  private boolean matchesAscii(byte[] data, int offset, String expected) {
    byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
    if (data.length < offset + expectedBytes.length) {
      return false;
    }
    for (int i = 0; i < expectedBytes.length; i++) {
      if (data[offset + i] != expectedBytes[i]) {
        return false;
      }
    }
    return true;
  }

  public record ValidationResult(String contentType) {}
}
