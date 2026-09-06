package com.vesit.openattend.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vesit.openattend.dto.admin.*;
import com.vesit.openattend.entity.*;
import com.vesit.openattend.entity.enums.Role;
import com.vesit.openattend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openattend.allowed-email-domain:ves.ac.in}")
    private String allowedEmailDomain;

    public RosterPreviewResponse previewRoster(List<RosterRowDto> inputRows) {
        if (inputRows == null || inputRows.isEmpty()) {
            return RosterPreviewResponse.builder()
                    .success(true)
                    .summary(Map.of("total", 0, "create", 0, "update", 0, "error", 0))
                    .rows(Collections.emptyList())
                    .build();
        }

        List<RosterRowDto> evaluatedRows = new ArrayList<>();
        int createCount = 0;
        int updateCount = 0;
        int errorCount = 0;

        for (RosterRowDto row : inputRows) {
            String rollNo = row.getRollNo() != null ? row.getRollNo().trim().toUpperCase() : "";
            String email = row.getEmail() != null ? row.getEmail().trim().toLowerCase() : "";
            String name = row.getName() != null ? row.getName().trim() : "";

            if (rollNo.isEmpty() || name.isEmpty()) {
                evaluatedRows.add(RosterRowDto.builder()
                        .rollNo(rollNo)
                        .name(name)
                        .email(email)
                        .division(row.getDivision())
                        .batch(row.getBatch())
                        .status("ERROR")
                        .reason("Missing roll number or name")
                        .build());
                errorCount++;
                continue;
            }

            if (!email.endsWith("@" + allowedEmailDomain)) {
                evaluatedRows.add(RosterRowDto.builder()
                        .rollNo(rollNo)
                        .name(name)
                        .email(email)
                        .division(row.getDivision())
                        .batch(row.getBatch())
                        .status("ERROR")
                        .reason("Invalid email domain (must be @" + allowedEmailDomain + ")")
                        .build());
                errorCount++;
                continue;
            }

            boolean exists = studentRepository.existsByRollNo(rollNo);
            if (exists) {
                evaluatedRows.add(RosterRowDto.builder()
                        .rollNo(rollNo)
                        .name(name)
                        .email(email)
                        .division(row.getDivision())
                        .batch(row.getBatch())
                        .status("UPDATE")
                        .build());
                updateCount++;
            } else {
                evaluatedRows.add(RosterRowDto.builder()
                        .rollNo(rollNo)
                        .name(name)
                        .email(email)
                        .division(row.getDivision())
                        .batch(row.getBatch())
                        .status("CREATE")
                        .build());
                createCount++;
            }
        }

        return RosterPreviewResponse.builder()
                .success(true)
                .summary(Map.of(
                        "total", inputRows.size(),
                        "create", createCount,
                        "update", updateCount,
                        "error", errorCount
                ))
                .rows(evaluatedRows)
                .build();
    }

    @Transactional
    public int commitRoster(List<RosterRowDto> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }

        int committed = 0;
        for (RosterRowDto row : rows) {
            if ("ERROR".equals(row.getStatus())) {
                continue;
            }

            String rollNo = row.getRollNo().trim().toUpperCase();
            String email = row.getEmail().trim().toLowerCase();

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> userRepository.save(User.builder()
                            .id(UUID.randomUUID().toString())
                            .email(email)
                            .role(Role.STUDENT)
                            .isActive(true)
                            .build()));

            Student student = studentRepository.findByRollNo(rollNo)
                    .orElseGet(() -> Student.builder()
                            .id(UUID.randomUUID().toString())
                            .user(user)
                            .rollNo(rollNo)
                            .build());

            student.setName(row.getName());
            student.setDivision(row.getDivision());
            student.setBatch(row.getBatch());
            studentRepository.save(student);

            committed++;
        }
        return committed;
    }

    public List<SyncLogResponse> getSyncLogs(Pageable pageable) {
        List<SyncLog> logs = syncLogRepository.findAllByOrderByStartedAtDesc(pageable != null ? pageable : PageRequest.of(0, 20));
        List<SyncLogResponse> result = new ArrayList<>();

        for (SyncLog l : logs) {
            String source = l.getSourceIdentifier() != null ? l.getSourceIdentifier() : "System Audit";

            result.add(SyncLogResponse.builder()
                    .id(l.getId())
                    .timestamp(l.getStartedAt() != null ? l.getStartedAt().toString() : "")
                    .sheet(source)
                    .status(l.getStatus().name())
                    .rowsRead(l.getRowsRead())
                    .rowsUpserted(l.getRowsUpserted())
                    .durationMs(l.getDurationMs())
                    .detail(l.getErrorMessage())
                    .build());
        }

        return result;
    }
}
