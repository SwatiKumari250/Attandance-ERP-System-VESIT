package com.vesit.openattend.repository;

import com.vesit.openattend.entity.StudentSubjectEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSubjectEnrollmentRepository extends JpaRepository<StudentSubjectEnrollment, String> {

    List<StudentSubjectEnrollment> findBySubjectId(String subjectId);

    List<StudentSubjectEnrollment> findByStudentId(String studentId);

    boolean existsByStudentIdAndSubjectId(String studentId, String subjectId);
}
