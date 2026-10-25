package com.infotact.aerosaga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infotact.aerosaga.dto.LoginRequest;
import com.infotact.aerosaga.dto.RegisterRequest;
import com.infotact.aerosaga.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AeroSagaIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully login seeded admin and receive valid JWT")
    void testAdminLogin() throws Exception {
        LoginRequest login = new LoginRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertThat(responseJson).contains("Bearer");
    }

    @Test
    @DisplayName("Should reject unauthorized requests to protected endpoints")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/drones"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should access protected drone endpoints with valid JWT Bearer token")
    void testAuthenticatedDroneAccess() throws Exception {
        // 1. Login
        LoginRequest login = new LoginRequest("admin", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // 2. Query drones with Bearer token
        mockMvc.perform(get("/api/drones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should register new operator, issue JWT and access protected APIs")
    void testRegisterNewUser() throws Exception {
        String username = "pilot_" + System.currentTimeMillis();
        RegisterRequest register = new RegisterRequest(username, "password123", User.Role.OPERATOR);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        // Access protected endpoint with new token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }
}
