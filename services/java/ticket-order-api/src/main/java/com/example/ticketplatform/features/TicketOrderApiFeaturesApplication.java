package com.example.ticketplatform.features;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {
      "com.example.ticketplatform.api",
      "com.example.ticketplatform.features"
    })
public class TicketOrderApiFeaturesApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(TicketOrderApiFeaturesApplication.class);
    application.setAdditionalProfiles("features");
    application.run(args);
  }
}
