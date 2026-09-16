package com.example.ticketplatform.api.application.port.in;

public record PatchEventDetailsCommand(
    String description, Integer numberOfPlaces, Integer numberOfRows, Integer seatsPerRow) {}
