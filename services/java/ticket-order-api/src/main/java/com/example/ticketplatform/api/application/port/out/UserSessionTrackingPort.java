package com.example.ticketplatform.api.application.port.out;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;

public interface UserSessionTrackingPort {

  void register(UUID userId, HttpSession session);

  void unregister(UUID userId, HttpSession session);
}
