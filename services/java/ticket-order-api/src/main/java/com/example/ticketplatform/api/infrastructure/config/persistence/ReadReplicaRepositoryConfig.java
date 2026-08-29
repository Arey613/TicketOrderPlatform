package com.example.ticketplatform.api.infrastructure.config.persistence;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.ticketplatform.api.adapter.out.persistence",
    includeFilters = @Filter(ReadReplicaRepository.class),
    entityManagerFactoryRef = "readReplicaEntityManagerFactory",
    transactionManagerRef = "readReplicaTransactionManager")
class ReadReplicaRepositoryConfig {}
