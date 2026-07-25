package com.example.ticketplatform.api.adapter.in.web;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.generated.contract.model.UserResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserContractMapper {

    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserResponse toContract(User user);

    default OffsetDateTime map(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
