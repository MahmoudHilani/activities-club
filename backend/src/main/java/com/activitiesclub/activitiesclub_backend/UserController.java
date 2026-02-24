package com.activitiesclub.activitiesclub_backend;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Add security deps (spring-boot-starter-security, JWT lib like jjwt).
// Stop returning User entity directly (it exposes passwordHash); use DTOs.
// Add a password encoder bean.
// Build POST /api/auth/register and POST /api/auth/login.
// Add JWT service (generate + validate token).
// Add security config: stateless, /api/auth/** public, others authenticated.
// Add GET /api/users/me using authenticated principal.

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> list() {
    return userRepository.findAll();
    }

    @PostMapping
    public User create(@RequestBody User body) {
        return userRepository.save(body);
    }
}
