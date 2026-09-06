package com.vesit.openattend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesit.openattend.dto.admin.RosterRowDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testEndToEndAdminRosterFlow() throws Exception {
        // Step 1: Roster Preview & Diffing
        List<RosterRowDto> roster = List.of(
                RosterRowDto.builder().rollNo("2024CS55").name("Admin Test Student").email("student55@ves.ac.in").division("D12A").batch("D12A-B1").build()
        );

        mockMvc.perform(post("/api/v1/admin/roster/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roster)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary.create").value(1));

        // Step 2: Commit Roster
        mockMvc.perform(post("/api/v1/admin/roster/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roster)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // Step 3: View Audit Logs
        mockMvc.perform(get("/api/v1/admin/sync/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.logs").isArray());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentRoleForbiddenOnAdminEndpoints() throws Exception {
        List<RosterRowDto> roster = List.of(
                RosterRowDto.builder().rollNo("2024CS99").name("Unauthorized").email("unauth@ves.ac.in").build()
        );
        mockMvc.perform(post("/api/v1/admin/roster/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roster)))
                .andExpect(status().isForbidden());
    }
}
