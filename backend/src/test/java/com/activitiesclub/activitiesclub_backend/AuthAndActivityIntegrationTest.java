package com.activitiesclub.activitiesclub_backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndActivityIntegrationTest {
    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedRoutesRequireBearerToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndActivityFlowWorksWithBearerToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "alice",
                      "email": "Alice@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "ALICE@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String token = readTextField(loginResult, "token");

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"))
            .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(post("/api/activities")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Chess Night",
                      "description": "Weekly chess meetup",
                      "startAt": "2026-03-20T18:00:00Z",
                      "endAt": "2026-03-20T20:00:00Z",
                      "locationName": "Student Center",
                      "locationAddress": "Main Campus",
                      "capacity": 20,
                      "visibility": "PUBLIC",
                      "reservationOpensAt": "2026-03-18T18:00:00Z",
                      "reservationClosesAt": "2026-03-20T17:00:00Z"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Chess Night"))
            .andExpect(jsonPath("$.organizer.username").value("alice"))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/activities")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("Chess Night"));
    }

    @Test
    void publicActivitiesEndpointOnlyReturnsPublishedPublicActivities() throws Exception {
        String token = registerAndLogin("organizer", "organizer@example.com", "password123");
        long publishedPublicId = createActivity(token, "Open Mic", "PUBLIC");
        createActivity(token, "Draft Event", "PUBLIC");
        long publishedPrivateId = createActivity(token, "Private Planning", "PRIVATE");

        publishActivity(token, publishedPublicId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        publishActivity(token, publishedPrivateId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/activities/public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].title").value("Open Mic"));
    }

    @Test
    void publishRequiresActivityOwnership() throws Exception {
        String ownerToken = registerAndLogin("owner", "owner@example.com", "password123");
        String otherUserToken = registerAndLogin("viewer", "viewer@example.com", "password123");
        long activityId = createActivity(ownerToken, "Board Games", "PUBLIC");

        publishActivity(otherUserToken, activityId)
            .andExpect(status().isForbidden());
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

        mockMvc.perform(options("/api/activities")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, org.hamcrest.Matchers.containsString("authorization")));
    }

    private String registerAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "email": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());

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

    private long createActivity(String token, String title, String visibility) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/activities")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "description": "Activity description",
                      "startAt": "2026-03-20T18:00:00Z",
                      "endAt": "2026-03-20T20:00:00Z",
                      "locationName": "Student Center",
                      "locationAddress": "Main Campus",
                      "capacity": 20,
                      "visibility": "%s",
                      "reservationOpensAt": "2026-03-18T18:00:00Z",
                      "reservationClosesAt": "2026-03-20T17:00:00Z"
                    }
                    """.formatted(title, visibility)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is(title)))
            .andReturn();

        return readLongField(createResult, "id");
    }

    private org.springframework.test.web.servlet.ResultActions publishActivity(String token, long activityId) throws Exception {
        return mockMvc.perform(patch("/api/activities/{activityId}/publish", activityId)
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
