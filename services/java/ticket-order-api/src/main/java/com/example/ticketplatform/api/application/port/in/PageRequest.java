package com.example.ticketplatform.api.application.port.in;

public record PageRequest(int page, int size, String sort) {}
