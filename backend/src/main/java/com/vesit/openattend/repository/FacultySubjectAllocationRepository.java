package com.vesit.openattend.repository;

import com.vesit.openattend.entity.FacultySubjectAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacultySubjectAllocationRepository extends JpaRepository<FacultySubjectAllocation, String> {

    List<FacultySubjectAllocation> findByFacultyId(String facultyId);

    List<FacultySubjectAllocation> findBySubjectIdAndDivision(String subjectId, String division);

    List<FacultySubjectAllocation> findByDivision(String division);
}
