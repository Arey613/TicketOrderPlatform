package com.example.ticketplatform.api.adapter.out.persistence.user;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class UserEntityMappingTest {

  @Test
  void alignsUserEntityToAppUsersTable() throws NoSuchFieldException {
    assertThat(UserEntity.class.getAnnotation(Table.class).name()).isEqualTo("app_users");
    assertThat(columnName("id")).isEqualTo("id");
    assertThat(columnName("email")).isEqualTo("email");
    assertThat(columnName("passwordHash")).isEqualTo("password_hash");
    assertThat(columnName("role")).isEqualTo("role");
    assertThat(columnName("enabled")).isEqualTo("enabled");
    assertThat(columnName("createdAt")).isEqualTo("created_at");
    assertThat(columnName("updatedAt")).isEqualTo("updated_at");
  }

  private String columnName(String fieldName) throws NoSuchFieldException {
    return UserEntity.class.getDeclaredField(fieldName).getAnnotation(Column.class).name();
  }
}
