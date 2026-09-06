package com.vesit.openattend.dto.faculty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceCorrectionRequest {

    @NotBlank(message = "Correction reason is mandatory")
    @Size(min = 10, message = "Correction reason must be at least 10 characters")
    private String correctionReason;

    private String topicCovered;

    @Builder.Default
    private List<String> updatedAbsentStudentIds = new ArrayList<>();
}
