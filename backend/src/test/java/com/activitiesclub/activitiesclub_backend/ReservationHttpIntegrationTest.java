package com.activitiesclub.activitiesclub_backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;

import com.activitiesclub.activitiesclub_backend.auth.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ReservationHttpIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void authenticatedStudentCanReserveOverRealHttp() throws Exception {
        User admin = createUser("admin", "admin@example.com", UserType.STAFF, true);
        User student = createUser("alice", "alice@example.com", UserType.STUDENT, false);

        Activity activity = new Activity();
        activity.setTitle("Workshop");
        activity.setDescription("Activity description");
        activity.setOrganizer(admin);
        activity.setImagePath("placeholder-activity.svg");
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setVisibility(ActivityVisibility.PUBLIC);
        activity.setCapacity(5);
        activity.setTicketPrice(java.math.BigDecimal.TEN);
        activity.setStartAt(Instant.now().plusSeconds(7200));
        activity.setEndAt(Instant.now().plusSeconds(10800));
        activity.setReservationOpensAt(Instant.now().minusSeconds(3600));
        activity.setReservationClosesAt(Instant.now().plusSeconds(3600));
        activity = activityRepository.save(activity);

        String token = jwtService.generate(student);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest meRequest = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/users/me")))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
        HttpResponse<String> meResponse = client.send(meRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(meResponse.statusCode()).isEqualTo(200);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/activities/" + activity.getId() + "/reservations")))
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        assertThat(root.path("status").asText()).isEqualTo("RESERVED");
    }

    @Test
    void overnightReservationRejectsUnderageStudentBeforeWaitlistAssignment() throws Exception {
        User admin = createUser("admin-underage", "admin-underage@example.com", UserType.STAFF, true);
        User adultStudent = createUser("adult-student", "adult@example.com", UserType.STUDENT, false);
        User student = createUser("young-student", "young@example.com", UserType.STUDENT, false);
        student.setDateOfBirth(LocalDate.of(2013, 3, 20));
        userRepository.save(student);

        Activity activity = createReservableActivity(admin, 1);
        activity.setOvernight(true);
        activity.setStartAt(Instant.parse("2030-03-20T18:00:00Z"));
        activity = activityRepository.save(activity);
        assertThat(reserve(activity, adultStudent).statusCode()).isEqualTo(200);

        HttpResponse<String> response = reserve(activity, student);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(reservationRepository.countByActivityIdAndStatus(activity.getId(), ReservationStatus.WAITLISTED))
            .isZero();
    }

    @Test
    void overnightReservationAcceptsStudentExactlyEighteenOnActivityStartDate() throws Exception {
        User admin = createUser("admin-eighteen", "admin-eighteen@example.com", UserType.STAFF, true);
        User student = createUser("eighteen-student", "eighteen@example.com", UserType.STUDENT, false);
        student.setDateOfBirth(LocalDate.of(2012, 3, 20));
        userRepository.save(student);

        Activity activity = createReservableActivity(admin, 5);
        activity.setOvernight(true);
        activity.setStartAt(Instant.parse("2030-03-20T18:00:00Z"));
        activity = activityRepository.save(activity);

        HttpResponse<String> response = reserve(activity, student);

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        assertThat(root.path("status").asText()).isEqualTo("RESERVED");
    }

    @Test
    void overnightReservationAllowsUnknownStudentDateOfBirth() throws Exception {
        User admin = createUser("admin-day", "admin-day@example.com", UserType.STAFF, true);
        User student = createUser("day-student", "day@example.com", UserType.STUDENT, false);
        student.setDateOfBirth(null);
        userRepository.save(student);

        Activity activity = createReservableActivity(admin, 5);
        activity.setOvernight(true);
        activity.setStartAt(Instant.parse("2030-03-20T18:00:00Z"));
        activity = activityRepository.save(activity);

        HttpResponse<String> response = reserve(activity, student);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private User createUser(String username, String email, UserType userType, boolean isAdmin) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setUserType(userType);
        user.setAdmin(isAdmin);
        user.setStudentNumber(userType == UserType.STUDENT ? "student-" + username : null);
        user.setPhoneNumber(userType == UserType.STUDENT ? "phone-" + username : null);
        user.setDateOfBirth(userType == UserType.STUDENT ? LocalDate.of(2000, 1, 1) : null);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }

    private Activity createReservableActivity(User admin, int capacity) {
        Activity activity = new Activity();
        activity.setTitle("Workshop");
        activity.setDescription("Activity description");
        activity.setOrganizer(admin);
        activity.setImagePath("placeholder-activity.svg");
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setVisibility(ActivityVisibility.PUBLIC);
        activity.setCapacity(capacity);
        activity.setTicketPrice(java.math.BigDecimal.TEN);
        activity.setStartAt(Instant.now().plusSeconds(7200));
        activity.setEndAt(Instant.now().plusSeconds(10800));
        activity.setReservationOpensAt(Instant.now().minusSeconds(3600));
        activity.setReservationClosesAt(Instant.now().plusSeconds(3600));
        return activityRepository.save(activity);
    }

    private HttpResponse<String> reserve(Activity activity, User student) throws Exception {
        String token = jwtService.generate(student);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url("/api/activities/" + activity.getId() + "/reservations")))
            .header("Authorization", "Bearer " + token)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
