package com.vesit.openattend;

import com.vesit.openattend.entity.*;
import com.vesit.openattend.entity.enums.AttendanceStatus;
import com.vesit.openattend.entity.enums.Role;
import com.vesit.openattend.entity.enums.SyncRunStatus;
import com.vesit.openattend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private FacultySubjectAllocationRepository allocationRepository;

    @Autowired
    private StudentSubjectEnrollmentRepository enrollmentRepository;

    @Autowired
    private LectureSessionRepository lectureSessionRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private AttendanceHistoryEventRepository attendanceHistoryEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Test
    void testERPEntityLifecycleAndRelationships() {
        // 1. Users (Faculty & Student)
        User facultyUser = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .email("prof.erp@ves.ac.in")
                .passwordHash("hashed_faculty")
                .role(Role.FACULTY)
                .build());

        User studentUser = userRepository.save(User.builder()
                .id(UUID.randomUUID().toString())
                .email("student.erp@ves.ac.in")
                .passwordHash("hashed_student")
                .role(Role.STUDENT)
                .build());

        // 2. Student
        Student student = studentRepository.save(Student.builder()
                .id(UUID.randomUUID().toString())
                .user(studentUser)
                .rollNo("2024CS99")
                .name("Vedant Gharat")
                .division("D12B")
                .batch("B1")
                .build());

        // 3. Subject
        Subject subject = subjectRepository.save(Subject.builder()
                .id(UUID.randomUUID().toString())
                .code("REP401")
                .name("Data Structures & Algorithms")
                .totalPlanned(45)
                .build());

        // 4. Faculty Allocation
        FacultySubjectAllocation allocation = allocationRepository.save(FacultySubjectAllocation.builder()
                .id(UUID.randomUUID().toString())
                .faculty(facultyUser)
                .subject(subject)
                .division("D12B")
                .batch("B1")
                .academicYear("2025-26")
                .semester(6)
                .build());
        assertNotNull(allocation.getId());

        // 5. Student Enrollment
        StudentSubjectEnrollment enrollment = enrollmentRepository.save(StudentSubjectEnrollment.builder()
                .id(UUID.randomUUID().toString())
                .student(student)
                .subject(subject)
                .academicYear("2025-26")
                .semester(6)
                .build());
        assertNotNull(enrollment.getId());

        // 6. Lecture Session
        LectureSession session = lectureSessionRepository.save(LectureSession.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .faculty(facultyUser)
                .division("D12B")
                .batch("B1")
                .sessionDate(LocalDate.of(2026, 8, 5))
                .sessionIndex(1)
                .sessionType("LECTURE")
                .topicCovered("Red-Black Trees")
                .totalStudents(1)
                .presentCount(1)
                .absentCount(0)
                .build());
        assertNotNull(session.getId());

        // 7. Attendance Record linked to Lecture Session
        AttendanceRecord record = attendanceRecordRepository.save(AttendanceRecord.builder()
                .id(UUID.randomUUID().toString())
                .student(student)
                .subject(subject)
                .lectureSession(session)
                .lectureDate(LocalDate.of(2026, 8, 5))
                .sessionIndex(1)
                .status(AttendanceStatus.PRESENT)
                .faculty(facultyUser.getEmail())
                .remarks("Red-Black Trees")
                .build());
        assertNotNull(record.getId());

        // 8. Attendance History Event
        AttendanceHistoryEvent event = attendanceHistoryEventRepository.save(AttendanceHistoryEvent.builder()
                .id(UUID.randomUUID().toString())
                .attendanceRecord(record)
                .previousStatus(null)
                .newStatus(AttendanceStatus.PRESENT)
                .build());
        assertNotNull(event.getId());

        // 9. Sync/Audit Log
        SyncLog syncLog = syncLogRepository.save(SyncLog.builder()
                .id(UUID.randomUUID().toString())
                .sourceIdentifier("FACULTY_SUBMIT_REP401")
                .status(SyncRunStatus.SUCCESS)
                .rowsRead(1)
                .rowsUpserted(1)
                .build());
        assertNotNull(syncLog.getId());

        // Assertions
        Optional<User> fetchedUser = userRepository.findByEmail("student.erp@ves.ac.in");
        assertTrue(fetchedUser.isPresent());
        assertEquals(Role.STUDENT, fetchedUser.get().getRole());

        Optional<Student> fetchedStudent = studentRepository.findByRollNo("2024CS99");
        assertTrue(fetchedStudent.isPresent());
        assertEquals("Vedant Gharat", fetchedStudent.get().getName());

        Optional<AttendanceRecord> fetchedRecord = attendanceRecordRepository
                .findByStudentIdAndSubjectIdAndLectureDateAndSessionIndex(
                        student.getId(),
                        subject.getId(),
                        LocalDate.of(2026, 8, 5),
                        1
                );
        assertTrue(fetchedRecord.isPresent());
        assertEquals(AttendanceStatus.PRESENT, fetchedRecord.get().getStatus());
        assertNotNull(fetchedRecord.get().getLectureSession());
        assertEquals("REP401", fetchedRecord.get().getLectureSession().getSubject().getCode());
    }
}
