package com.activitiesclub.activitiesclub_backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndActivityIntegrationTest {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.activitiesclub.activitiesclub_backend.auth.JwtService jwtService;

    @Test
    void protectedRoutesRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registerSupportsStudentAndStaffUsersAndAdminEndpointsRequireAdmin() throws Exception {
        String adminToken = createAdminToken();
        String studentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");
        String staffToken = registerAndLoginStaff("sam", "sam@example.com", "password123");

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userType").value("STAFF"))
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.studentNumber").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.phoneNumber").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userType").value("STUDENT"))
            .andExpect(jsonPath("$.isAdmin").value(false))
            .andExpect(jsonPath("$.studentNumber").value("student-alice"))
            .andExpect(jsonPath("$.phoneNumber").value("phone-alice"));

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + staffToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userType").value("STAFF"))
            .andExpect(jsonPath("$.isAdmin").value(false))
            .andExpect(jsonPath("$.studentNumber").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$.phoneNumber").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/admin/activities")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/activities")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    @Test
    void adminCanListUsersAndToggleAdminAccess() throws Exception {
        String adminToken = createAdminToken();
        User managedUser = createUser("staff-member", "staff@example.com", UserType.STAFF, false);

        mockMvc.perform(get("/api/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].username").value("staff-member"))
            .andExpect(jsonPath("$[0].userType").value("STAFF"))
            .andExpect(jsonPath("$[0].isAdmin").value(false));

        mockMvc.perform(patch("/api/admin/users/{userId}/admin", managedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "isAdmin": true
                    }
                    """)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(managedUser.getId()))
            .andExpect(jsonPath("$.isAdmin").value(true))
            .andExpect(jsonPath("$.userType").value("STAFF"));
    }

    @Test
    void adminCannotChangeOwnAccess() throws Exception {
        User currentAdmin = createUser("admin", "admin@example.com", UserType.STAFF, true);
        String adminToken = tokenFor(currentAdmin);

        mockMvc.perform(patch("/api/admin/users/{userId}/admin", currentAdmin.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "isAdmin": false
                    }
                    """)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isConflict());
    }

    @Test
    void adminUpdateRequiresExplicitIsAdminFlag() throws Exception {
        String adminToken = createAdminToken();
        User managedUser = createUser("staff-member", "staff@example.com", UserType.STAFF, false);

        mockMvc.perform(patch("/api/admin/users/{userId}/admin", managedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanCreateUpdateAndDeleteDraftActivitiesWithImages() throws Exception {
        String adminToken = createAdminToken();
        long activityId = createAdminActivity(adminToken, "Chess Night", "image/png", "poster.png", "PUBLIC", 20, "15.50");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/admin/activities/{activityId}", activityId)
                .file(activityJsonPart("Board Game Night", "PRIVATE", 25, "0"))
                .file(imageFile("image/webp", "updated.webp", "updated-image"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Board Game Night"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.ticketPrice").value(0))
            .andExpect(jsonPath("$.imageUrl").isString());

        mockMvc.perform(delete("/api/admin/activities/{activityId}", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void createValidatesUploadTypeSizeAndTimeWindow() throws Exception {
        String adminToken = createAdminToken();

        mockMvc.perform(multipart("/api/admin/activities")
                .file(activityJsonPart("Bad Image", "PUBLIC", 20, "10.00"))
                .file(imageFile("text/plain", "poster.txt", "not-an-image"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest());

        byte[] oversized = new byte[(5 * 1024 * 1024) + 1];
        mockMvc.perform(multipart("/api/admin/activities")
                .file(activityJsonPart("Huge Image", "PUBLIC", 20, "10.00"))
                .file(new MockMultipartFile("image", "big.png", "image/png", oversized))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/admin/activities")
                .file(activityJsonPart(Map.of(
                    "title", "Broken Times",
                    "ticketPrice", BigDecimal.valueOf(5.00),
                    "visibility", "PUBLIC",
                    "startAt", "2030-03-20T20:00:00Z",
                    "endAt", "2030-03-20T18:00:00Z"
                )))
                .file(imageFile("image/png", "poster.png", "poster"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void publicActivitiesEndpointOnlyReturnsPublishedPublicActivitiesAndIncludesCurrentUserReservationState() throws Exception {
        String adminToken = createAdminToken();
        String studentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");
        long publicActivityId = createAdminActivity(adminToken, "Open Mic", "image/png", "poster.png", "PUBLIC", 2, "0");
        long privateActivityId = createAdminActivity(adminToken, "Private Planning", "image/png", "poster.png", "PRIVATE", 2, "0");

        publishActivity(adminToken, publicActivityId).andExpect(status().isOk());
        publishActivity(adminToken, privateActivityId).andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{activityId}/reservations", publicActivityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(get("/api/activities/public")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("Open Mic"))
            .andExpect(jsonPath("$.content[0].imageUrl").isString())
            .andExpect(jsonPath("$.content[0].ticketPrice").value(0))
            .andExpect(jsonPath("$.content[0].confirmedReservationCount").value(1))
            .andExpect(jsonPath("$.content[0].currentUserReservationStatus").value("RESERVED"));
    }

    @Test
    void publicActivityDetailEndpointReturnsPublishedPublicActivityForAnonymousUsers() throws Exception {
        String adminToken = createAdminToken();
        long activityId = createAdminActivity(adminToken, "Open Mic", "image/png", "poster.png", "PUBLIC", 2, "0");

        publishActivity(adminToken, activityId).andExpect(status().isOk());

        mockMvc.perform(get("/api/activities/{activityId}", activityId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(activityId))
            .andExpect(jsonPath("$.title").value("Open Mic"))
            .andExpect(jsonPath("$.imageUrl").isString())
            .andExpect(jsonPath("$.currentUserReservationStatus").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void publicActivityDetailEndpointIgnoresInvalidBearerTokenAndFallsBackToAnonymous() throws Exception {
        String adminToken = createAdminToken();
        long activityId = createAdminActivity(adminToken, "Open Mic", "image/png", "poster.png", "PUBLIC", 2, "0");

        publishActivity(adminToken, activityId).andExpect(status().isOk());

        mockMvc.perform(get("/api/activities/{activityId}", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-invalid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(activityId))
            .andExpect(jsonPath("$.currentUserReservationStatus").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void publicActivityDetailEndpointIncludesCurrentUserReservationStateForAuthenticatedUsers() throws Exception {
        String adminToken = createAdminToken();
        String studentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");
        long activityId = createAdminActivity(adminToken, "Workshop", "image/png", "poster.png", "PUBLIC", 2, "0");

        publishActivity(adminToken, activityId).andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(get("/api/activities/{activityId}", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(activityId))
            .andExpect(jsonPath("$.confirmedReservationCount").value(1))
            .andExpect(jsonPath("$.currentUserReservationStatus").value("RESERVED"));
    }

    @Test
    void publicActivityDetailEndpointReturnsNotFoundForNonPublicActivities() throws Exception {
        String adminToken = createAdminToken();
        long publicActivityId = createAdminActivity(adminToken, "Cancelled Event", "image/png", "poster.png", "PUBLIC", 2, "0");
        long privateActivityId = createAdminActivity(adminToken, "Private Planning", "image/png", "poster.png", "PRIVATE", 2, "0");
        long draftActivityId = createAdminActivity(adminToken, "Draft Workshop", "image/png", "poster.png", "PUBLIC", 2, "0");

        publishActivity(adminToken, publicActivityId).andExpect(status().isOk());
        publishActivity(adminToken, privateActivityId).andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/activities/{activityId}/cancel", publicActivityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/activities/{activityId}", privateActivityId))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/activities/{activityId}", draftActivityId))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/activities/{activityId}", publicActivityId))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/activities/{activityId}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    void waitlistReservationIsPromotedWhenConfirmedReservationIsCancelled() throws Exception {
        String adminToken = createAdminToken();
        String firstStudentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");
        String secondStudentToken = registerAndLoginStudent("bob", "bob@example.com", "password123");

        long activityId = createAdminActivity(adminToken, "Workshop", "image/png", "poster.png", "PUBLIC", 1, "25.00");
        publishActivity(adminToken, activityId).andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITLISTED"))
            .andExpect(jsonPath("$.waitlistCount").value(1));

        mockMvc.perform(delete("/api/activities/{activityId}/reservations/me", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/activities/public")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].currentUserReservationStatus").value("RESERVED"))
            .andExpect(jsonPath("$.content[0].confirmedReservationCount").value(1))
            .andExpect(jsonPath("$.content[0].waitlistCount").value(0))
            .andExpect(jsonPath("$.content[0].atCapacity").value(true));
    }

    @Test
    void adminCanFetchReservationRosterForActivity() throws Exception {
        String adminToken = createAdminToken();
        String firstStudentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");
        String secondStudentToken = registerAndLoginStudent("bob", "bob@example.com", "password123");
        String thirdStudentToken = registerAndLoginStudent("cara", "cara@example.com", "password123");

        long activityId = createAdminActivity(adminToken, "Workshop", "image/png", "poster.png", "PUBLIC", 1, "25.00");
        publishActivity(adminToken, activityId).andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITLISTED"));

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + thirdStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WAITLISTED"));

        mockMvc.perform(delete("/api/activities/{activityId}/reservations/me", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstStudentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/admin/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activity.id").value(activityId))
            .andExpect(jsonPath("$.activity.confirmedReservationCount").value(1))
            .andExpect(jsonPath("$.activity.waitlistCount").value(1))
            .andExpect(jsonPath("$.reservations", hasSize(3)))
            .andExpect(jsonPath("$.reservations[0].status").value("RESERVED"))
            .andExpect(jsonPath("$.reservations[0].user.username").value("bob"))
            .andExpect(jsonPath("$.reservations[0].user.email").value("bob@example.com"))
            .andExpect(jsonPath("$.reservations[0].reservedAt").isString())
            .andExpect(jsonPath("$.reservations[1].status").value("WAITLISTED"))
            .andExpect(jsonPath("$.reservations[1].user.username").value("cara"))
            .andExpect(jsonPath("$.reservations[1].user.email").value("cara@example.com"))
            .andExpect(jsonPath("$.reservations[2].status").value("CANCELLED"))
            .andExpect(jsonPath("$.reservations[2].user.username").value("alice"))
            .andExpect(jsonPath("$.reservations[2].user.email").value("alice@example.com"))
            .andExpect(jsonPath("$.reservations[2].cancelledAt").isString());
    }

    @Test
    void reservationRosterRequiresAdminAndReturnsNotFoundForUnknownActivity() throws Exception {
        String adminToken = createAdminToken();
        String studentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");
        long activityId = createAdminActivity(adminToken, "Chess Night", "image/png", "poster.png", "PUBLIC", 10, "0");

        mockMvc.perform(get("/api/admin/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/activities/{activityId}/reservations", Long.MAX_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelBlocksDeletionAfterReservationHistoryExists() throws Exception {
        String adminToken = createAdminToken();
        String studentToken = registerAndLoginStudent("alice", "alice@example.com", "password123");

        long activityId = createAdminActivity(adminToken, "Cinema Night", "image/png", "poster.png", "PUBLIC", 5, "8.00");
        publishActivity(adminToken, activityId).andExpect(status().isOk());

        mockMvc.perform(post("/api/activities/{activityId}/reservations", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/activities/{activityId}", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/admin/activities/{activityId}/cancel", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelledActivitiesCanBeRepublishedAndDeletedWithoutReservationHistory() throws Exception {
        String adminToken = createAdminToken();

        long activityId = createAdminActivity(adminToken, "Cinema Night", "image/png", "poster.png", "PUBLIC", 5, "8.00");

        mockMvc.perform(patch("/api/admin/activities/{activityId}/cancel", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        publishActivity(adminToken, activityId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(patch("/api/admin/activities/{activityId}/cancel", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(delete("/api/admin/activities/{activityId}", activityId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void corsAllowsFrontendOriginForPublicAndAuthenticatedApiCalls() throws Exception {
        mockMvc.perform(get("/api/activities/public")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN));

        mockMvc.perform(options("/api/auth/login")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("POST")));

        mockMvc.perform(options("/api/admin/activities")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, org.hamcrest.Matchers.containsString("PUT")))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("authorization")));
    }

    private String registerAndLoginStudent(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "email": "%s",
                      "userType": "STUDENT",
                      "studentNumber": "%s",
                      "phoneNumber": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, email, "student-" + username, "phone-" + username, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());

        return login(email, password);
    }

    private String registerAndLoginStaff(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "email": "%s",
                      "userType": "STAFF",
                      "password": "%s"
                    }
                    """.formatted(username, email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());

        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        return readTextField(loginResult, "token");
    }

    private String createAdminToken() {
        return tokenFor(createUser("admin", "admin@example.com", UserType.STAFF, true));
    }

    private String tokenFor(User user) {
        return jwtService.generate(user);
    }

    private User createUser(String username, String email, UserType userType, boolean isAdmin) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setUserType(userType);
        user.setAdmin(isAdmin);
        user.setStudentNumber(userType == UserType.STUDENT ? "student-" + username : null);
        user.setPhoneNumber(userType == UserType.STUDENT ? "phone-" + username : null);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        return userRepository.save(user);
    }

    private long createAdminActivity(
        String token,
        String title,
        String imageContentType,
        String filename,
        String visibility,
        int capacity,
        String ticketPrice
    ) throws Exception {
        MvcResult createResult = mockMvc.perform(multipart("/api/admin/activities")
                .file(activityJsonPart(title, visibility, capacity, ticketPrice))
                .file(imageFile(imageContentType, filename, "sample-image"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is(title)))
            .andReturn();

        return readLongField(createResult, "id");
    }

    private MockMultipartFile activityJsonPart(String title, String visibility, int capacity, String ticketPrice) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", "Activity description");
        payload.put("startAt", "2030-03-20T18:00:00Z");
        payload.put("endAt", "2030-03-20T20:00:00Z");
        payload.put("locationName", "Student Center");
        payload.put("locationAddress", "Main Campus");
        payload.put("capacity", capacity);
        payload.put("ticketPrice", new BigDecimal(ticketPrice));
        payload.put("visibility", visibility);
        payload.put("reservationOpensAt", "2026-01-10T18:00:00Z");
        payload.put("reservationClosesAt", "2030-03-20T17:00:00Z");
        return activityJsonPart(payload);
    }

    private MockMultipartFile activityJsonPart(Map<String, Object> payload) {
        try {
            return new MockMultipartFile(
                "activity",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                OBJECT_MAPPER.writeValueAsBytes(payload)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize test activity payload", exception);
        }
    }

    private MockMultipartFile imageFile(String contentType, String filename, String body) {
        return new MockMultipartFile(
            "image",
            filename,
            contentType,
            body.getBytes(StandardCharsets.UTF_8)
        );
    }

    private org.springframework.test.web.servlet.ResultActions publishActivity(String token, long activityId) throws Exception {
        return mockMvc.perform(patch("/api/admin/activities/{activityId}/publish", activityId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private long readLongField(MvcResult result, String fieldName) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return root.path(fieldName).asLong();
    }

    private String readTextField(MvcResult result, String fieldName) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        return root.path(fieldName).asText();
    }
}
