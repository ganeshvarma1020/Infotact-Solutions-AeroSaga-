package com.infotact.aerosaga.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;

/**
 * End-to-end through HTTP: Spring Boot app + security + H2 database + real workflow
 * running on Temporal's in-memory test server (no external Temporal needed).
 */
@SpringBootTest(properties = {
        "aerosaga.temporal.connect=false",
        "aerosaga.simulation.time-scale=0",
        "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1",
        "spring.security.user.name=tester",
        "spring.security.user.password=secret"
})
@AutoConfigureMockMvc
@Import(MissionApiTest.InMemoryTemporal.class)
class MissionApiTest {

    @TestConfiguration
    static class InMemoryTemporal {
        @Bean(destroyMethod = "close")
        TestWorkflowEnvironment testWorkflowEnvironment() {
            return TestWorkflowEnvironment.newInstance();
        }

        @Bean
        WorkflowClient workflowClient(TestWorkflowEnvironment env) {
            return env.getWorkflowClient();
        }
    }

    private static final String GOOD_MISSION = """
            {"operator":"api-test","payloadKg":1.0,"scenario":"NONE",
             "waypoints":[{"name":"Bommasandra","lat":12.8169,"lon":77.6950},
                          {"name":"Hosur Road Hub","lat":12.8700,"lon":77.6400}]}""";

    @Autowired
    private MockMvc mvc;

    /** The app context (and its fleet) is shared between tests: start each test with full batteries. */
    @org.junit.jupiter.api.BeforeEach
    void rechargeFleet() throws Exception {
        for (String drone : new String[] {"DR-01", "DR-02", "DR-03"}) {
            mvc.perform(authed(post("/api/fleet/" + drone + "/recharge")).with(csrf())).andExpect(status().isNoContent());
        }
    }

    private MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder b) {
        return b.with(httpBasic("tester", "secret"));
    }

    private String createMission(String json) throws Exception {
        String body = mvc.perform(authed(post("/api/missions")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.missionId");
    }

    /** Waits until the DATABASE row shows a final phase (the journal writes it just after the workflow decides). */
    private String waitForFinalPhase(String id) throws Exception {
        for (int i = 0; i < 150; i++) {
            String body = mvc.perform(authed(get("/api/missions"))).andReturn().getResponse().getContentAsString();
            java.util.List<String> phases = JsonPath.read(body, "$[?(@.id == '" + id + "')].phase");
            if (!phases.isEmpty()) {
                String phase = phases.get(0);
                if (phase.equals("COMPLETED") || phase.equals("ROLLED_BACK") || phase.equals("ABORTED") || phase.equals("REJECTED")) {
                    return phase;
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError("mission " + id + " did not finish in time");
    }

    @Test
    void pingAndHealthArePublic() throws Exception {
        mvc.perform(get("/api/ping")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void apiRequiresLogin() throws Exception {
        mvc.perform(get("/api/missions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/missions").with(httpBasic("tester", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(authed(post("/api/missions")).contentType(MediaType.APPLICATION_JSON).content(GOOD_MISSION))
                .andExpect(status().isForbidden());
    }

    @Test
    void missionRunsToCompletionAndIsStored() throws Exception {
        String id = createMission(GOOD_MISSION);
        assertTrue(id.startsWith("M-"));
        assertEquals("COMPLETED", waitForFinalPhase(id));

        mvc.perform(authed(get("/api/missions/" + id + "/events")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phase").value("VALIDATING"));
        mvc.perform(authed(get("/api/missions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].phase").value("COMPLETED"));
    }

    @Test
    void scenarioFailureIsRolledBackThroughTheApi() throws Exception {
        String id = createMission(GOOD_MISSION.replace("\"NONE\"", "\"LOW_BATTERY\""));
        assertEquals("ROLLED_BACK", waitForFinalPhase(id));
        mvc.perform(authed(get("/api/missions/" + id)))
                .andExpect(jsonPath("$.failureCode").value("LOW_BATTERY"));
    }

    @Test
    void planInsideNoFlyZoneIsRejectedWith422() throws Exception {
        String hal = """
                {"operator":"api-test","payloadKg":1.0,
                 "waypoints":[{"name":"HAL","lat":12.9500,"lon":77.6682}]}""";
        mvc.perform(authed(post("/api/missions")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(hal))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("no-fly zone")));
    }

    @Test
    void malformedBodyIsRejectedWith400() throws Exception {
        String missingOperator = """
                {"payloadKg":1.0,"waypoints":[{"name":"A","lat":12.82,"lon":77.69}]}""";
        mvc.perform(authed(post("/api/missions")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(missingOperator))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownMissionIs404AndFinishedMissionCannotBeAborted() throws Exception {
        mvc.perform(authed(get("/api/missions/M-NOPE"))).andExpect(status().isNotFound());

        String id = createMission(GOOD_MISSION);
        waitForFinalPhase(id);
        mvc.perform(authed(post("/api/missions/" + id + "/abort")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"too late\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void fleetConfigAndCsrfEndpointsWork() throws Exception {
        mvc.perform(authed(get("/api/fleet"))).andExpect(status().isOk()).andExpect(jsonPath("$.drones.length()").value(3));
        mvc.perform(authed(get("/api/config"))).andExpect(status().isOk()).andExpect(jsonPath("$.noFlyZones[0].name").value("HAL Airport"));
        mvc.perform(authed(get("/api/csrf"))).andExpect(status().isOk()).andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
    }
}
