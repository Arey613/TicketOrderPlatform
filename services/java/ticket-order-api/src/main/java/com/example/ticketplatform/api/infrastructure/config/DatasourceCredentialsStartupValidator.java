package com.example.ticketplatform.api.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DatasourceCredentialsStartupValidator implements ApplicationRunner {

  private static final String LOCAL_PROFILE = "local";
  private static final String KNOWN_LOCAL_DEFAULT = "ticket_order";
  private static final String[] CREDENTIAL_PROPERTIES = {
    "spring.datasource.primary.username",
    "spring.datasource.primary.password",
    "spring.datasource.read-replica.username",
    "spring.datasource.read-replica.password"
  };

  private final Environment environment;

  @Override
  public void run(ApplicationArguments args) {
    if (environment.acceptsProfiles(Profiles.of(LOCAL_PROFILE))) {
      return;
    }

    for (String property : CREDENTIAL_PROPERTIES) {
      String value = environment.getProperty(property);
      if (value == null || value.isBlank()) {
        throw new IllegalStateException("Required datasource property is missing: " + property);
      }
      if (KNOWN_LOCAL_DEFAULT.equals(value)) {
        throw new IllegalStateException(
            "Datasource property must not use the known local default value: " + property);
      }
    }
  }
}
