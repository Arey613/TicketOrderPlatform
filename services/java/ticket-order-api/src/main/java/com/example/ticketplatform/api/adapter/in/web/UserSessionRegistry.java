package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.application.port.out.UserSessionRevocationPort;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class UserSessionRegistry implements UserSessionRevocationPort {

  private final Map<UUID, Set<HttpSession>> sessionsByUser = new ConcurrentHashMap<>();

  void register(UUID userId, HttpSession session) {
    sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
  }

  void unregister(UUID userId, HttpSession session) {
    Set<HttpSession> sessions = sessionsByUser.get(userId);
    if (sessions == null) {
      return;
    }
    sessions.remove(session);
    if (sessions.isEmpty()) {
      sessionsByUser.remove(userId);
    }
  }

  @Override
  public void revokeSessions(UUID userId) {
    Set<HttpSession> sessions = sessionsByUser.remove(userId);
    if (sessions == null) {
      return;
    }
    sessions.forEach(this::invalidate);
  }

  private void invalidate(HttpSession session) {
    try {
      session.invalidate();
    } catch (IllegalStateException exception) {
      // Session was already invalidated by logout, timeout, or another revocation path.
    }
  }
}
