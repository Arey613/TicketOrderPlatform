package com.example.ticketplatform.api.infrastructure.config.observability.correlation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CorrelationId {

  public static final String HEADER_NAME = "X-Correlation-ID";
  static final String MDC_KEY = "correlationId";
  public static final String DEFAULT_VALUE_TEMPLATE = "{uuid}-{date:dd-MM-yyyy}";
  public static final String DEFAULT_VALIDATION_PATTERN =
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-\\d{2}-\\d{2}-\\d{4}$";

  private static final int UUID_LENGTH = 36;
  private static final int VALUE_LENGTH = 47;
  private static final Pattern DATE_TOKEN_PATTERN = Pattern.compile("\\{date:([^}]+)}");
  private static final Pattern RANDOM_HEX_TOKEN_PATTERN = Pattern.compile("\\{random-hex:(\\d+)}");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

  private CorrelationId() {}

  static String generate(Instant currentTime) {
    return generate(currentTime, DEFAULT_VALUE_TEMPLATE, ZoneId.of("UTC"));
  }

  static String generate(Instant currentTime, String valueTemplate, ZoneId zoneId) {
    if (valueTemplate == null || valueTemplate.isBlank()) {
      return generate(currentTime, DEFAULT_VALUE_TEMPLATE, zoneId);
    }

    String generatedValue = valueTemplate.replace("{uuid}", UUID.randomUUID().toString());
    generatedValue = replaceDateTokens(generatedValue, currentTime, zoneId);
    generatedValue = replaceRandomHexTokens(generatedValue);
    return generatedValue;
  }

  static boolean isValid(String value, String validationPattern) {
    if (value == null || validationPattern == null || validationPattern.isBlank()) {
      return false;
    }

    if (containsWhitespaceOrControlCharacter(value) || !matchesPattern(value, validationPattern)) {
      return false;
    }

    if (!DEFAULT_VALIDATION_PATTERN.equals(validationPattern)) {
      return true;
    }

    try {
      if (value.length() != VALUE_LENGTH) {
        return false;
      }
      UUID.fromString(value.substring(0, UUID_LENGTH));
      LocalDate.parse(value.substring(UUID_LENGTH + 1), DATE_FORMATTER);
      return true;
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private static boolean matchesPattern(String value, String validationPattern) {
    try {
      return Pattern.compile(validationPattern).matcher(value).matches();
    } catch (RuntimeException exception) {
      return false;
    }
  }

  private static boolean containsWhitespaceOrControlCharacter(String value) {
    return value.chars()
        .anyMatch(character -> Character.isWhitespace(character) || Character.isISOControl(character));
  }

  private static String replaceDateTokens(String value, Instant currentTime, ZoneId zoneId) {
    Matcher matcher = DATE_TOKEN_PATTERN.matcher(value);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      DateTimeFormatter formatter =
          DateTimeFormatter.ofPattern(matcher.group(1).replace("yyyy", "uuuu"), Locale.ROOT)
              .withResolverStyle(ResolverStyle.STRICT);
      LocalDate currentDate = LocalDate.ofInstant(currentTime, zoneId);
      matcher.appendReplacement(result, Matcher.quoteReplacement(formatter.format(currentDate)));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  private static String replaceRandomHexTokens(String value) {
    Matcher matcher = RANDOM_HEX_TOKEN_PATTERN.matcher(value);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      int length = Integer.parseInt(matcher.group(1));
      String randomHex = UUID.randomUUID().toString().replace("-", "");
      matcher.appendReplacement(
          result, Matcher.quoteReplacement(randomHex.substring(0, Math.min(length, randomHex.length()))));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
