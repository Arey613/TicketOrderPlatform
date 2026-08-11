package com.example.ticketplatform.api.application.port.in;

public record EventDetailsCommand(
    String description, Integer numberOfPlaces, Integer numberOfRows, Integer seatsPerRow) {}
