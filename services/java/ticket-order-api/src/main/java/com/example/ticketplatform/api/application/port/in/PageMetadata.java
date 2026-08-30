package com.example.ticketplatform.api.application.port.in;

public record PageMetadata(
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public static PageMetadata of(int number, int size, long totalElements) {
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    return new PageMetadata(
        number,
        size,
        totalElements,
        totalPages,
        number == 0,
        totalPages == 0 || number >= totalPages - 1);
  }
}
