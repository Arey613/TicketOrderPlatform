package com.example.ticketplatform.api;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.example.ticketplatform.api",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String SECURITY_CONTEXT_HOLDER =
      "org.springframework.security.core.context.SecurityContextHolder";

  @ArchTest
  static final ArchRule domainDependsOnlyOnDomainAndJdk =
      noClasses()
          .that()
          .resideInAPackage("..api.domain..")
          .should()
          .dependOnClassesThat(
              not(resideInAPackage("..api.domain.."))
                  .and(not(resideInAPackage("java..")))
                  .and(not(resideInAPackage("lombok.."))));

  @ArchTest
  static final ArchRule applicationDoesNotDependOnAdaptersOrInfrastructure =
      noClasses()
          .that()
          .resideInAPackage("..api.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api.adapter..", "..api.infrastructure..");

  @ArchTest
  static final ArchRule applicationDoesNotDependOnPersistenceOrSecurityFrameworks =
      noClasses()
          .that()
          .resideInAPackage("..api.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework.dao..",
              "org.springframework.jdbc..",
              "org.springframework.orm..",
              "org.springframework.security..",
              "org.springframework.transaction",
              "org.springframework.transaction.support..",
              "jakarta.persistence..");

  @ArchTest
  static final ArchRule inboundAdapterDoesNotDependOnOutboundAdapterOrServices =
      noClasses()
          .that()
          .resideInAPackage("..api.adapter.in..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api.adapter.out..", "..api.application.service..");

  // TODO: move AuthenticatedUserPrincipal and YamlPropertySourceFactory out of
  // infrastructure.config so the inbound adapter has no infrastructure exceptions.
  @ArchTest
  static final ArchRule inboundAdapterDependsOnInfrastructureOnlyForKnownExceptions =
      noClasses()
          .that()
          .resideInAPackage("..api.adapter.in..")
          .should()
          .dependOnClassesThat(
              resideInAPackage("..api.infrastructure..")
                  .and(not(simpleName("AuthenticatedUserPrincipal")))
                  .and(not(simpleName("YamlPropertySourceFactory"))));

  @ArchTest
  static final ArchRule outboundAdapterDoesNotDependOnInboundAdapterOrServices =
      noClasses()
          .that()
          .resideInAPackage("..api.adapter.out..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api.adapter.in..", "..api.application.service..");

  @ArchTest
  static final ArchRule infrastructureDoesNotDependOnAdapters =
      noClasses()
          .that()
          .resideInAPackage("..api.infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api.adapter..");

  @ArchTest
  static final ArchRule onlyDesignatedClassesReadSecurityContextHolder =
      noClasses()
          .that()
          .doNotHaveSimpleName("CurrentUserProvider")
          .and()
          .doNotHaveSimpleName("AuthenticationSessionManager")
          .and()
          .doNotHaveSimpleName("CorrelationMdcFilter")
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName(SECURITY_CONTEXT_HOLDER);
}
