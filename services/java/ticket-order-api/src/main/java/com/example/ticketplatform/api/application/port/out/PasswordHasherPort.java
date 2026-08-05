package com.example.ticketplatform.api.application.port.out;

public interface PasswordHasherPort {

  String hash(String rawPassword);
}
