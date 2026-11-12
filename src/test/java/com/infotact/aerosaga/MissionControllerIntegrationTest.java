package com.infotact.aerosaga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/drones returns HTTP 200 with drone fleet JSON")
    void testGetDrones() throws Exception {
        mockMvc.perform(get("/api/drones")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @DisplayName("GET /api/dashboard/summary returns operational statistics")
    void testGetDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDrones").exists())
                .andExpect(jsonPath("$.restrictedZones").isArray());
    }

    @Test
    @DisplayName("GET /api/dashboard/scenarios returns preset demonstration list")
    void testGetScenarios() throws Exception {
        mockMvc.perform(get("/api/dashboard/scenarios")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("SCENARIO_1_STANDARD_DELIVERY"))
                .andExpect(jsonPath("$[2].id").value("SCENARIO_3_WEATHER_SAGA"));
    }
}
