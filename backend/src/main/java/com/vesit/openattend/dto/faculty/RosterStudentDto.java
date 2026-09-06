package com.vesit.openattend.dto.faculty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterStudentDto {
    private String studentId;
    private String rollNo;
    private String name;
    private String division;
    private String batch;
}
