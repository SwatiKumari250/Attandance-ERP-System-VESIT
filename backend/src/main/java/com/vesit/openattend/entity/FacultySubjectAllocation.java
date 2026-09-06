package com.vesit.openattend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "faculty_subject_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_faculty_subject_alloc",
            columnNames = {"faculty_id", "subject_id", "division", "batch", "academic_year", "semester"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultySubjectAllocation {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private User faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 32)
    private String division;

    @Column(length = 32)
    private String batch;

    @Column(name = "academic_year", nullable = false, length = 32)
    @Builder.Default
    private String academicYear = "2025-26";

    @Column(nullable = false)
    @Builder.Default
    private Integer semester = 6;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
