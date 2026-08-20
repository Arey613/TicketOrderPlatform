package com.example.ticketplatform.api.infrastructure.config.persistence;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateSettings;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.ticketplatform.api.adapter.out.persistence",
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager")
class PersistenceConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource.primary")
  DataSourceProperties primaryDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource.primary.hikari")
  HikariDataSource primaryDataSource(
      @Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @ConfigurationProperties("spring.datasource.analytical")
  DataSourceProperties analyticalDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @ConfigurationProperties("spring.datasource.analytical.hikari")
  HikariDataSource analyticalDataSource(
      @Qualifier("analyticalDataSourceProperties") DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @Primary
  LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
      EntityManagerFactoryBuilder builder,
      @Qualifier("primaryDataSource") DataSource dataSource,
      JpaProperties jpaProperties,
      HibernateProperties hibernateProperties) {
    return builder
        .dataSource(dataSource)
        .packages("com.example.ticketplatform.api.adapter.out.persistence")
        .persistenceUnit("primary")
        .properties(
            hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(), new HibernateSettings()))
        .build();
  }

  @Bean
  @Primary
  PlatformTransactionManager primaryTransactionManager(
      @Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }
}
