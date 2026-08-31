package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.infrastructure.config.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(PaginationProperties.class)
@PropertySource(value = "classpath:pagination.yml", factory = YamlPropertySourceFactory.class)
class PaginationConfig {}
