package com.example.ticketplatform.api.application.port.out;

import java.util.UUID;

public interface UserSessionRevocationPort {

  void revokeSessions(UUID userId);
}
