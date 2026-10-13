package com.example.ticketplatform.api.adapter.in.web;

import com.example.ticketplatform.api.infrastructure.config.YamlPropertySourceFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties(UserUpdateValidationProperties.class)
@PropertySource(value = "classpath:user-update-validation.yml", factory = YamlPropertySourceFactory.class)
class UserUpdateValidationConfig {}
