package com.activitiesclub.activitiesclub_backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void emailLookupsAreCaseInsensitive() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("Alice@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.STUDENT);
        userRepository.save(user);

        assertThat(userRepository.existsByEmailIgnoreCase("alice@example.com")).isTrue();
        assertThat(userRepository.findByEmailIgnoreCase("ALICE@EXAMPLE.COM"))
            .map(User::getUsername)
            .contains("alice");
    }
}
