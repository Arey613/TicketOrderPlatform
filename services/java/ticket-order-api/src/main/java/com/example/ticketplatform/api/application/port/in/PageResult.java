package com.example.ticketplatform.api.application.port.in;

import java.util.List;

public record PageResult<T>(List<T> items, PageMetadata page) {}
