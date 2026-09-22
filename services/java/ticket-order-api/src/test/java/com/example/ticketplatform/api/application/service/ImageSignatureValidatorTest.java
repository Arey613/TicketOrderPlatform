package com.example.ticketplatform.api.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketplatform.api.infrastructure.config.media.MediaProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageSignatureValidatorTest {

  private final ImageSignatureValidator validator =
      new ImageSignatureValidator(defaultMediaProperties());

  @Test
  void acceptsValidJpegAndSniffsContentType() {
    ImageSignatureValidator.ValidationResult result = validator.validate(validJpegBytes());

    assertThat(result.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  void acceptsValidPngAndSniffsContentType() {
    ImageSignatureValidator.ValidationResult result = validator.validate(validPngBytes());

    assertThat(result.contentType()).isEqualTo("image/png");
  }

  @Test
  void acceptsValidWebpAndSniffsContentType() {
    ImageSignatureValidator.ValidationResult result = validator.validate(validWebpBytes());

    assertThat(result.contentType()).isEqualTo("image/webp");
  }

  @Test
  void rejectsSvgBytes() {
    byte[] svg =
        "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
            .getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> validator.validate(svg)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsPlainTextBytes() {
    byte[] text = "just some plain text, not an image".getBytes(StandardCharsets.UTF_8);

    assertThatThrownBy(() -> validator.validate(text))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsCorruptBytesWithValidJpegSignatureButUndecodableBody() {
    byte[] jpeg = validJpegBytes();
    for (int i = 3; i < jpeg.length; i++) {
      jpeg[i] = 0x00;
    }

    assertThatThrownBy(() -> validator.validate(jpeg))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEmptyBytes() {
    assertThatThrownBy(() -> validator.validate(new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsOversizedImage() {
    byte[] oversized = new byte[5_242_881];

    assertThatThrownBy(() -> validator.validate(oversized))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sniffsContentTypeFromBytesRegardlessOfAnyDeclaredType() {
    byte[] png = validPngBytes();

    ImageSignatureValidator.ValidationResult result = validator.validate(png);

    assertThat(result.contentType()).isEqualTo("image/png");
  }

  private static MediaProperties defaultMediaProperties() {
    return new MediaProperties(null, null);
  }

  private static byte[] validJpegBytes() {
    return writeImage("jpg", BufferedImage.TYPE_INT_RGB);
  }

  private static byte[] validPngBytes() {
    return writeImage("png", BufferedImage.TYPE_INT_ARGB);
  }

  private static byte[] writeImage(String format, int imageType) {
    try {
      BufferedImage image = new BufferedImage(4, 4, imageType);
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      if (!ImageIO.write(image, format, output)) {
        throw new IllegalStateException("No writer available for format " + format);
      }
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to build test image", exception);
    }
  }

  private static byte[] validWebpBytes() {
    byte[] vp8Chunk = {'V', 'P', '8', ' ', 0x10, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03};
    int riffSize = 4 + vp8Chunk.length;
    List<Byte> bytes =
        new java.util.ArrayList<>(
            List.of(
                (byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F',
                (byte) (riffSize & 0xFF), (byte) ((riffSize >> 8) & 0xFF),
                (byte) ((riffSize >> 16) & 0xFF), (byte) ((riffSize >> 24) & 0xFF),
                (byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P'));
    for (byte b : vp8Chunk) {
      bytes.add(b);
    }
    byte[] result = new byte[bytes.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = bytes.get(i);
    }
    return result;
  }
}
