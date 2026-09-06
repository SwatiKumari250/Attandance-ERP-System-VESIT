package com.vesit.openattend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesit.openattend.dto.faculty.AttendanceCorrectionRequest;
import com.vesit.openattend.dto.faculty.AttendanceSubmissionRequest;
import com.vesit.openattend.entity.*;
import com.vesit.openattend.entity.enums.AttendanceStatus;
import com.vesit.openattend.entity.enums.Role;
import com.vesit.openattend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FacultyAttendanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private FacultySubjectAllocationRepository allocationRepository;

    @Autowired
    private LectureSessionRepository lectureSessionRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceHistoryEventRepository historyEventRepository;

    private User facultyUser;
    private Subject subject;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setup() {
        facultyUser = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .email("prof.sharma@ves.ac.in")
                .role(Role.FACULTY)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .id(UUID.randomUUID().toString())
                .code("FAC401")
                .name("Advanced Algorithms")
                .totalPlanned(40)
                .build());

        User u1 = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .email("std1@ves.ac.in")
                .role(Role.STUDENT)
                .build());

        student1 = studentRepository.save(Student.builder()
                .id(UUID.randomUUID().toString())
                .user(u1)
                .rollNo("2024FAC01")
                .name("Alice Smith")
                .division("D12A")
                .batch("B1")
                .build());

        User u2 = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .email("std2@ves.ac.in")
                .role(Role.STUDENT)
                .build());

        student2 = studentRepository.save(Student.builder()
                .id(UUID.randomUUID().toString())
                .user(u2)
                .rollNo("2024FAC02")
                .name("Bob Jones")
                .division("D12A")
                .batch("B1")
                .build());

        allocationRepository.save(FacultySubjectAllocation.builder()
                .id(UUID.randomUUID().toString())
                .faculty(facultyUser)
                .subject(subject)
                .division("D12A")
                .batch(null)
                .academicYear("2025-26")
                .semester(6)
                .build());
    }

    @Test
    @WithMockUser(username = "prof.sharma@ves.ac.in", roles = "FACULTY")
    void testGetMySubjectsAndRoster() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/my-subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subjects", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.subjects[0].subjectCode").value("FAC401"));

        mockMvc.perform(get("/api/v1/faculty/roster")
                .param("subjectId", subject.getId())
                .param("division", "D12A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.roster", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @WithMockUser(username = "prof.sharma@ves.ac.in", roles = "FACULTY")
    void testAbsenteesAttendanceSubmissionAndHistory() throws Exception {
        // Fast marking: student1 is absent, student2 is present (default)
        AttendanceSubmissionRequest req = AttendanceSubmissionRequest.builder()
                .subjectId(subject.getId())
                .division("D12A")
                .batch(null)
                .lectureDate(LocalDate.now())
                .sessionIndex(1)
                .sessionType("LECTURE")
                .topicCovered("Dynamic Programming Foundations")
                .absentStudentIds(List.of(student1.getId()))
                .build();

        mockMvc.perform(post("/api/v1/faculty/attendance/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.session.presentCount").value(1))
                .andExpect(jsonPath("$.session.absentCount").value(1));

        // Verify records in DB
        AttendanceRecord rec1 = attendanceRecordRepository
                .findByStudentIdAndSubjectIdAndLectureDateAndSessionIndex(student1.getId(), subject.getId(), LocalDate.now(), 1)
                .orElseThrow();
        assertEquals(AttendanceStatus.ABSENT, rec1.getStatus());

        AttendanceRecord rec2 = attendanceRecordRepository
                .findByStudentIdAndSubjectIdAndLectureDateAndSessionIndex(student2.getId(), subject.getId(), LocalDate.now(), 1)
                .orElseThrow();
        assertEquals(AttendanceStatus.PRESENT, rec2.getStatus());

        // Verify history events created
        List<AttendanceHistoryEvent> history1 = historyEventRepository.findByAttendanceRecordId(rec1.getId());
        assertFalse(history1.isEmpty());
        assertEquals(AttendanceStatus.ABSENT, history1.get(0).getNewStatus());
    }

    @Test
    @WithMockUser(username = "prof.sharma@ves.ac.in", roles = "FACULTY")
    void testCorrectionWindowEnforcesReasonAndUpdatesHistory() throws Exception {
        // Initial submission
        AttendanceSubmissionRequest subReq = AttendanceSubmissionRequest.builder()
                .subjectId(subject.getId())
                .division("D12A")
                .batch(null)
                .lectureDate(LocalDate.now())
                .sessionIndex(2)
                .sessionType("LECTURE")
                .topicCovered("Graph Algorithms")
                .absentStudentIds(List.of(student1.getId(), student2.getId()))
                .build();

        String responseStr = mockMvc.perform(post("/api/v1/faculty/attendance/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionId = objectMapper.readTree(responseStr).get("session").get("id").asText();

        // Attempt correction with too short reason (< 10 chars) -> 400 Bad Request
        AttendanceCorrectionRequest shortReasonReq = AttendanceCorrectionRequest.builder()
                .correctionReason("Wrong")
                .updatedAbsentStudentIds(List.of(student1.getId()))
                .build();

        mockMvc.perform(put("/api/v1/faculty/attendance/" + sessionId + "/correct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortReasonReq)))
                .andExpect(status().isBadRequest());

        // Valid correction: mark student2 as present with valid justification
        AttendanceCorrectionRequest validReq = AttendanceCorrectionRequest.builder()
                .correctionReason("Student arrived late with approved medical note from Dean")
                .updatedAbsentStudentIds(List.of(student1.getId())) // student2 no longer absent
                .topicCovered("Graph Algorithms - Bellman Ford")
                .build();

        mockMvc.perform(put("/api/v1/faculty/attendance/" + sessionId + "/correct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.presentCount").value(1))
                .andExpect(jsonPath("$.session.absentCount").value(1));

        // Verify student2 status updated to PRESENT
        AttendanceRecord rec2 = attendanceRecordRepository
                .findByStudentIdAndSubjectIdAndLectureDateAndSessionIndex(student2.getId(), subject.getId(), LocalDate.now(), 2)
                .orElseThrow();
        assertEquals(AttendanceStatus.PRESENT, rec2.getStatus());

        // Verify history has audit trail
        List<AttendanceHistoryEvent> history2 = historyEventRepository.findByAttendanceRecordId(rec2.getId());
        assertEquals(2, history2.size());
        assertEquals(AttendanceStatus.ABSENT, history2.get(1).getPreviousStatus());
        assertEquals(AttendanceStatus.PRESENT, history2.get(1).getNewStatus());
    }

    @Test
    @WithMockUser(username = "student@ves.ac.in", roles = "STUDENT")
    void testStudentRoleForbiddenOnFacultyEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/faculty/my-subjects"))
                .andExpect(status().isForbidden());
    }
}
