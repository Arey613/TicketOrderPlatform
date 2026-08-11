package com.example.ticketplatform.api.application.port.in;

public record EventDetailsCommand(
    String description, int numberOfPlaces, int numberOfRows, int seatsPerRow) {}
