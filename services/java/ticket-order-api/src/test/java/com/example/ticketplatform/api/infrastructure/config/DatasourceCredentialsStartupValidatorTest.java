package com.example.ticketplatform.api.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DatasourceCredentialsStartupValidatorTest {

  @Test
  void skipsValidationUnderLocalProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");
    DatasourceCredentialsStartupValidator validator =
        new DatasourceCredentialsStartupValidator(environment);

    assertThatNoException().isThrownBy(() -> validator.run(null));
  }

  @Test
  void failsWhenPrimaryPasswordIsBlankOutsideLocalProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("staging");
    environment.setProperty("spring.datasource.primary.username", "app_user");
    environment.setProperty("spring.datasource.primary.password", "");
    environment.setProperty("spring.datasource.read-replica.username", "app_user");
    environment.setProperty("spring.datasource.read-replica.password", "s3cret");
    DatasourceCredentialsStartupValidator validator =
        new DatasourceCredentialsStartupValidator(environment);

    assertThatIllegalStateException().isThrownBy(() -> validator.run(null));
  }

  @Test
  void failsWhenPrimaryUsernameIsMissingOutsideLocalProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("staging");
    environment.setProperty("spring.datasource.primary.password", "s3cret");
    environment.setProperty("spring.datasource.read-replica.username", "app_user");
    environment.setProperty("spring.datasource.read-replica.password", "s3cret");
    DatasourceCredentialsStartupValidator validator =
        new DatasourceCredentialsStartupValidator(environment);

    assertThatIllegalStateException().isThrownBy(() -> validator.run(null));
  }

  @Test
  void failsWhenCredentialMatchesKnownLocalDefaultOutsideLocalProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("staging");
    environment.setProperty("spring.datasource.primary.username", "app_user");
    environment.setProperty("spring.datasource.primary.password", "ticket_order");
    environment.setProperty("spring.datasource.read-replica.username", "app_user");
    environment.setProperty("spring.datasource.read-replica.password", "s3cret");
    DatasourceCredentialsStartupValidator validator =
        new DatasourceCredentialsStartupValidator(environment);

    assertThatIllegalStateException().isThrownBy(() -> validator.run(null));
  }

  @Test
  void passesWithRealCredentialsOutsideLocalProfile() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("staging");
    environment.setProperty("spring.datasource.primary.username", "app_user");
    environment.setProperty("spring.datasource.primary.password", "s3cret-primary");
    environment.setProperty("spring.datasource.read-replica.username", "app_user");
    environment.setProperty("spring.datasource.read-replica.password", "s3cret-replica");
    DatasourceCredentialsStartupValidator validator =
        new DatasourceCredentialsStartupValidator(environment);

    assertThatNoException().isThrownBy(() -> validator.run(null));
  }
}
