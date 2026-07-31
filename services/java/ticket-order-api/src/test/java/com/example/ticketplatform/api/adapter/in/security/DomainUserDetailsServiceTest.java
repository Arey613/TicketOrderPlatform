package com.example.ticketplatform.api.adapter.in.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.example.ticketplatform.api.application.port.out.UserRepositoryPort;
import com.example.ticketplatform.api.domain.model.user.User;
import com.example.ticketplatform.api.domain.model.user.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class DomainUserDetailsServiceTest {

    @Test
    void loadsUserDetailsFromUserRepositoryPort() {
        User user = new User(
                UUID.randomUUID(),
                "admin@example.com",
                "{noop}secret",
                UserRole.ADMIN,
                true,
                Instant.now(),
                Instant.now());
        DomainUserDetailsService service = new DomainUserDetailsService(new StubUserRepositoryPort(user));

        var userDetails = service.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("admin@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("{noop}secret");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        DomainUserDetailsService service = new DomainUserDetailsService(new StubUserRepositoryPort(null));

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private record StubUserRepositoryPort(User user) implements UserRepositoryPort {

        @Override
        public User save(User user) {
            return user;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(user);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(user).filter(value -> value.email().equals(email));
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email).isPresent();
        }
    }
}
