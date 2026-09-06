package com.vesit.openattend.dto.faculty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultySubjectDto {
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String division;
    private String batch;
    private int totalStudents;
    private String lastSessionDate;
}
