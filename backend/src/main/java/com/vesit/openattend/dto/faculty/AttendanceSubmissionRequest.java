package com.vesit.openattend.dto.faculty;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSubmissionRequest {

    @NotBlank(message = "Subject ID is required")
    private String subjectId;

    @NotBlank(message = "Division is required")
    private String division;

    private String batch;

    @Builder.Default
    private LocalDate lectureDate = LocalDate.now();

    @Builder.Default
    private Integer sessionIndex = 1;

    @Builder.Default
    private String sessionType = "LECTURE";

    private String topicCovered;

    @Builder.Default
    private List<String> absentStudentIds = new ArrayList<>();
}
