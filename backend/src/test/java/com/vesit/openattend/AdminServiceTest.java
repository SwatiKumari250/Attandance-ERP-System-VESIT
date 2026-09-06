package com.vesit.openattend;

import com.vesit.openattend.dto.admin.*;
import com.vesit.openattend.repository.StudentRepository;
import com.vesit.openattend.service.admin.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminServiceTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void testRosterPreviewAndCommit() {
        List<RosterRowDto> input = List.of(
                RosterRowDto.builder().rollNo("2024CS10").name("Aarav Sharma").email("aarav@ves.ac.in").division("D12A").batch("D12A-B1").build(),
                RosterRowDto.builder().rollNo("2024CS11").name("Invalid User").email("invalid@gmail.com").division("D12A").batch("D12A-B1").build(), // Invalid domain
                RosterRowDto.builder().rollNo("").name("Missing Roll").email("test@ves.ac.in").division("D12A").batch("D12A-B1").build()             // Missing roll
        );

        RosterPreviewResponse preview = adminService.previewRoster(input);
        assertTrue(preview.isSuccess());
        assertEquals(3, preview.getSummary().get("total"));
        assertEquals(1, preview.getSummary().get("create"));
        assertEquals(2, preview.getSummary().get("error"));

        int committed = adminService.commitRoster(preview.getRows());
        assertEquals(1, committed);

        assertTrue(studentRepository.existsByRollNo("2024CS10"));
    }
}
