package com.example.ticketplatform.api.adapter.out.persistence.user;

import com.example.ticketplatform.api.application.port.in.PageMetadata;
import com.example.ticketplatform.api.application.port.in.PageRequest;
import com.example.ticketplatform.api.application.port.in.PageResult;
import com.example.ticketplatform.api.domain.model.user.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UserPageMapper {

  private final UserMapper userMapper;

  PageResult<User> toPageResult(List<UserEntity> entities, Long totalElements, PageRequest pageRequest) {
    List<User> items = entities.stream().map(userMapper::toDomain).toList();
    int totalPages = (int) Math.ceil((double) totalElements / pageRequest.size());
    return new PageResult<>(
        items,
        new PageMetadata(
            pageRequest.page(),
            pageRequest.size(),
            totalElements,
            totalPages,
            pageRequest.page() == 0,
            pageRequest.page() + 1 >= totalPages));
  }
}
