package com.vesit.openattend.repository;

import com.vesit.openattend.entity.LectureSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LectureSessionRepository extends JpaRepository<LectureSession, String> {

    Optional<LectureSession> findBySubjectIdAndDivisionAndBatchAndSessionDateAndSessionIndex(
            String subjectId, String division, String batch, LocalDate sessionDate, Integer sessionIndex
    );

    Optional<LectureSession> findBySubjectIdAndDivisionAndSessionDateAndSessionIndexAndBatchIsNull(
            String subjectId, String division, LocalDate sessionDate, Integer sessionIndex
    );

    List<LectureSession> findByFacultyIdOrderBySessionDateDescSessionIndexDesc(String facultyId);

    List<LectureSession> findBySubjectIdOrderBySessionDateDescSessionIndexDesc(String subjectId);

    List<LectureSession> findByDivisionOrderBySessionDateDescSessionIndexDesc(String division);
}
