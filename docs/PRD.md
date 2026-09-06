# OpenAttend — Software Requirements Specification (SRS) / PRD

**Working Title:** OpenAttend — Native Attendance ERP System  
**Document Type:** Software Requirements Specification & Product Requirements Document  
**Target Institution:** Vivekanand Education Society's Institute of Technology (VESIT) & Engineering Colleges  
**Status:** Approved v2.0 (Native ERP Architecture)  
**Audience:** Engineering, Faculty, Open-Source Contributors  

---

## Table of Contents

1. [Overview & Vision](#1-overview--vision)
2. [User Personas & Role-Based Access Control](#2-user-personas--role-based-access-control)
3. [Core Functional Modules](#3-core-functional-modules)
4. [Non-Functional Requirements & Performance SLAs](#4-non-functional-requirements--performance-slas)
5. [System Architecture](#5-system-architecture)
6. [Database Schema (PostgreSQL)](#6-database-schema-postgresql)
7. [API Specifications](#7-api-specifications)
8. [Offline PWA & Resilience Strategy](#8-offline-pwa--resilience-strategy)
9. [Accreditation & NAAC Diary Specifications](#9-accreditation--naac-diary-specifications)
10. [Security & Institutional Compliance](#10-security--institutional-compliance)

---

## 1. Overview & Vision

### 1.1 Problem Statement
Engineering colleges historically rely on **Google Forms → Google Sheets** for lecture attendance. While familiar, this manual setup presents severe operational bottlenecks:
- **Wasted Teaching Time:** Marking 70+ students one-by-one in a 50-minute lecture takes 5–8 minutes of valuable classroom instruction time.
- **Accidental Submission Mistakes:** In Google Forms, there is no standardized, audited correction window. Mis-marked students suffer attendance deficits with zero paper trail.
- **Low Student Visibility:** Students cannot see live attendance percentages, aggregate defaulter risks, or actionable recovery counts without manually hunting through shared spreadsheets.
- **Administrative Burden:** Every month and before Unit Tests, Class Teachers and HODs spend countless hours compiling multiple subject spreadsheets to generate the mandatory **75% Attendance Defaulter List**.
- **Accreditation Chaos:** Maintaining the NAAC/NBA Academic Course Diary ("Topics Covered" tracker) requires redundant manual paper logging.

### 1.2 Solution
OpenAttend is a **complete, high-speed Native Attendance ERP System** that completely replaces the Google Form ecosystem:
1. **Semester Setup:** Class Teachers upload a single **Master Division Excel Spreadsheet** containing student records, lab batch divisions (A, B, C), and elective allocations.
2. **Subject Allocation:** Department Admins allocate specific subjects and lab batches to teachers.
3. **High-Speed Absentees Marking:** In the classroom, faculty use an ultra-fast **"Mark Absentees" UI**, tapping only the 3–5 absent students in under 10 seconds.
4. **48-Hour Audit Window:** Faculty can correct accidental marking errors within 48 hours with a mandatory justification reason.
5. **Real-Time Student Dashboard:** Students monitor their attendance on mobile via animated percentage rings, safe skips, and lecture history logs.
6. **1-Click Administrative Reporting:** Instant calculation and generation of the official **Printable A4 Defaulter Notice PDF** with VESIT letterhead and HOD signature blocks.
7. **Paperless Workflows:** Native digital portals for **Official Duty (OD) / Medical Leaves** and **Proxy Lecture Delegation**.

---

## 2. User Personas & Role-Based Access Control

OpenAttend defines four distinct institutional roles:

```
                  ┌──────────────────────┐
                  │     SUPER_ADMIN      │
                  └──────────┬───────────┘
                             │
                  ┌──────────▼───────────┐
                  │        ADMIN         │ (HOD / Academic Coordinator)
                  └──────────┬───────────┘
                             │
            ┌────────────────┴────────────────┐
            │                                 │
 ┌──────────▼───────────┐          ┌──────────▼───────────┐
 │    CLASS_TEACHER     │          │       FACULTY        │
 └──────────┬───────────┘          └──────────┬───────────┘
            │                                 │
            └────────────────┬────────────────┘
                             │
                  ┌──────────▼───────────┐
                  │       STUDENT        │
                  └──────────────────────┘
```

| Role | Responsibilities & Capabilities |
|---|---|
| `STUDENT` | Accesses live percentage dashboard, subject breakdown cards, Safe Skips predictor, lecture-by-lecture audit history, and submits digital OD leave applications. |
| `FACULTY` | Views assigned classes (`/my-subjects`), marks classroom attendance via the <10s absentees chip grid, logs lecture topics for NAAC diary, and edits sessions within the 48-hour correction window. |
| `CLASS_TEACHER` | Uploads the division's Master Semester Excel Roster at term start, monitors overall division attendance, unlocks past-48-hour sessions for their division, and generates official monthly defaulter notices. |
| `ADMIN` / `SUPER_ADMIN` | Manages faculty accounts, allocates subjects and lab batches, configures semester dates, and audits institutional attendance logs across all departments. |

---

## 3. Core Functional Modules

### Module 1: Master Semester Excel Roster Upload & Parsing (Issue #1)
- Endpoint: `POST /api/v1/admin/roster/upload` (accepts `.xlsx` and `.csv`).
- Class Teacher selects Division (e.g. `D15B`), Semester, and Academic Year (e.g. `2026-27`).
- Apache POI parser automatically extracts:
  - `Roll No`: Unique student identifier within the division.
  - `Student Name`: Full name.
  - `Email`: Institutional `@ves.ac.in` address.
  - `Lab Batch`: `A`, `B`, or `C`.
  - `Elective`: Student's chosen course (e.g. *Cloud Computing*, *ADMT*, *Soft Computing*).
- Atomically creates `User` (password defaulted to email) and `Student` records, linking them to core compulsory subjects and elective tracks in `student_subject_enrollments`.

### Module 2: Faculty-to-Subject & Batch Allocation (Issue #2)
- Table: `faculty_subject_allocations`.
- Admin assigns Faculty to Subject + Division + Batch (e.g. *Prof. Rao -> Operating Systems -> D15B -> ALL* for theory, and *Prof. Patil -> OS Lab -> D15B -> Batch B* for practicals).
- Endpoint `GET /api/v1/faculty/my-subjects` returns only designated classes.
- RBAC Pre-Authorization: Teachers cannot view or mark attendance for unallocated subjects (`403 Forbidden`).

### Module 3: High-Speed Attendance Marking ("Mark Absentees" Mode) (Issues #3 & #4)
- Classroom Marking UI displays enrolled students as interactive chips (default: **Present**).
- Faculty tap only the absent students, turning them **Red with an Absent badge**.
- Live counter displays summary: `Total: 71 | Present: 67 | Absent: 4 (Roll: 3, 6, 8, 11)`.
- Optional field: **Topic Covered** (e.g. *Process Synchronization & Mutex Locks*).
- Submits to `POST /api/v1/faculty/attendance/submit` in `<100ms`.
- Generates `LectureSession` with SHA-256 session hash to guarantee idempotency against double taps.

### Module 4: 48-Hour Audit & Correction Window (Issue #5)
- Endpoint: `PUT /api/v1/faculty/attendance/{sessionId}/correct`.
- For 48 hours post-session, the subject teacher can edit attendance status.
- Every edit requires a mandatory justification reason (`@NotBlank`, min 10 characters).
- Appends an immutable change log to `attendance_history_events` recording `modified_by`, `timestamp`, `previous_status`, `new_status`, and `reason`.
- After 48 hours, the session automatically locks.

### Module 5: 1-Click Defaulter List Generator & Official A4 PDF (Issues #6 & #7)
- Fast SQL aggregation calculates total conducted, attended, and percentages per student and subject.
- Exports in JSON, `.xlsx`, and official **Printable A4 PDF**:
  - College Header: *Vivekanand Education Society's Institute of Technology (VESIT)*.
  - Color-coded bands: 🔴 Critical (`< 65%`), 🟡 Warning (`65% – 74.9%`), 🟢 Safe (`≥ 75%`).
  - Formal signature blocks for Class Teacher, Attendance Coordinator, and HOD.

### Module 6: NAAC / NBA Academic Course Diary (Issue #8)
- Every lecture records `topic_covered`, `lecture_type` (`THEORY`, `LAB`, `REMEDIAL`, `TUTORIAL`), and headcounts.
- Endpoint `GET /api/v1/faculty/diary?subjectId=...` compiles syllabus completion metrics and average attendance rates for direct export into NAAC Course Files.

### Module 7: Mobile-First Student Experience & Predictor (Issues #9 & #10)
- Animated circular percentage ring showing overall and subject-level attendance.
- **Safe Skips & Recovery Formulas:**
  - Safe Skips ($P \ge 75\%$): $S = \lfloor \frac{4A - 3T}{3} \rfloor$
  - Must-Attend Recovery ($P < 75\%$): $R = \lceil 3T - 4A \rceil$

### Module 8: Automated 75% Defaulter Warning Notifications (Issue #11)
- Detects drops below the 75% boundary upon lecture submission.
- Dispatches institutional alert email to `@ves.ac.in` with current %, missed subjects, and recovery count.
- Enforces a **7-day cooldown** to prevent notification spam across consecutive lectures.

### Module 9: Paperless OD / Medical Leave Portal (Issue #12)
- Students apply digitally with event details and proof attachment.
- Coordinator / Teacher approves with 1-click.
- Approved leaves automatically transition sessions to `DUTY_PRESENT` ($\frac{\text{Attended} + \text{OD}}{\text{Total}}$).

### Module 10: Proxy / Substitute Lecture Delegation (Issue #13)
- Endpoint `POST /api/v1/faculty/proxy/assign` allows delegating a lecture slot to a substitute teacher with an audit record (`is_proxy = true`).

---

## 4. Non-Functional Requirements & Performance SLAs

| Requirement | Target Metric / SLA | Architectural Enforcement |
|---|---|---|
| **Classroom Marking Speed** | Entire class marked in **< 10 seconds** | Absentees-only chip mode (90%+ students default present). |
| **Submission Latency** | API response in **< 100 ms** | Optimized single-transaction PostgreSQL batch write. |
| **Student Dashboard Load** | **< 50 ms TTFB** | Direct indexed PostgreSQL queries; zero external sync latency. |
| **Idempotency** | **Zero duplicate records** on network retry | Unique natural key `(lecture_session_id, student_id)` + SHA-256 session hash. |
| **Audit Compliance** | 100% immutable edit trail | `attendance_history_events` append-only table. |
| **Offline Resilience** | Full offline marking capability | PWA Service Worker + IndexedDB queue with auto-sync on reconnect. |
| **Domain Lockdown** | 100% rejection of unauthorized emails | Strict regex verification enforcing `@ves.ac.in`. |

---

## 5. Database Schema (PostgreSQL)

```sql
-- Users & Roles
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'STUDENT',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Students Profile
CREATE TABLE students (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    roll_no VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    division VARCHAR(32) NOT NULL,
    batch VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Subjects & Courses
CREATE TABLE subjects (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    total_planned INT DEFAULT 45,
    is_elective BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Faculty Allocations (Issue #2)
CREATE TABLE faculty_subject_allocations (
    id VARCHAR(36) PRIMARY KEY,
    faculty_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id VARCHAR(36) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    division VARCHAR(32) NOT NULL,
    batch VARCHAR(32) NOT NULL DEFAULT 'ALL',
    semester VARCHAR(16),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Student Enrollments (Issue #1)
CREATE TABLE student_subject_enrollments (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject_id VARCHAR(36) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    division VARCHAR(32) NOT NULL,
    batch VARCHAR(32),
    is_elective BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_subject UNIQUE (student_id, subject_id)
);

-- Conducted Lecture Sessions (Issue #4 & #8)
CREATE TABLE lecture_sessions (
    id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(36) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    faculty_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    division VARCHAR(32) NOT NULL,
    batch VARCHAR(32) NOT NULL DEFAULT 'ALL',
    lecture_date DATE NOT NULL,
    session_index INT NOT NULL DEFAULT 1,
    lecture_hours INT NOT NULL DEFAULT 1,
    topic_covered VARCHAR(512),
    lecture_type VARCHAR(32) NOT NULL DEFAULT 'THEORY',
    is_proxy BOOLEAN NOT NULL DEFAULT FALSE,
    proxy_faculty_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    session_hash VARCHAR(64) NOT NULL,
    locked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session_slot UNIQUE (subject_id, division, batch, lecture_date, session_index)
);

-- Atomic Student Attendance Records (Issue #4)
CREATE TABLE attendance_records (
    id VARCHAR(36) PRIMARY KEY,
    lecture_session_id VARCHAR(36) NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'PRESENT',
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_session_student UNIQUE (lecture_session_id, student_id)
);

-- 48-Hour Audit History Events (Issue #5)
CREATE TABLE attendance_history_events (
    id VARCHAR(36) PRIMARY KEY,
    attendance_record_id VARCHAR(36) NOT NULL REFERENCES attendance_records(id) ON DELETE CASCADE,
    previous_status VARCHAR(32) NOT NULL,
    new_status VARCHAR(32) NOT NULL,
    modified_by VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Official Duty (OD) Leave Requests (Issue #12)
CREATE TABLE od_leave_requests (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    event_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    proof_url VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by VARCHAR(36) REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Deduplicated Notifications (Issue #11)
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject_id VARCHAR(36) REFERENCES subjects(id) ON DELETE SET NULL,
    type VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 6. API Specifications

### 6.1 Authentication
- `POST /api/v1/auth/login`: Authenticates institutional email and password; returns JWT token + user role profile.
- `GET /api/v1/auth/me`: Returns active user profile and roles.

### 6.2 Faculty & Attendance Operations
- `GET /api/v1/faculty/my-subjects`: Returns list of classes assigned to the authenticated faculty member.
- `GET /api/v1/faculty/roster?subjectId=...&division=...&batch=...`: Returns student chips list for marking screen.
- `POST /api/v1/faculty/attendance/submit`: Fast absentees submission endpoint.
  ```json
  {
    "subjectId": "subj_os_01",
    "division": "D15B",
    "batch": "ALL",
    "lectureDate": "2026-09-07",
    "sessionIndex": 1,
    "lectureHours": 1,
    "absentRollNumbers": [3, 7, 12],
    "topicCovered": "Deadlock Detection & Recovery Algorithms",
    "lectureType": "THEORY"
  }
  ```
- `PUT /api/v1/faculty/attendance/{sessionId}/correct`: 48-hour correction endpoint.
  ```json
  {
    "studentRollNo": 7,
    "newStatus": "PRESENT",
    "reason": "Student arrived late during roll call due to lab equipment issue."
  }
  ```
- `POST /api/v1/faculty/proxy/assign`: Delegate lecture to a substitute colleague.
- `GET /api/v1/faculty/diary?subjectId=...`: Course diary syllabus report.

### 6.3 Student Experience
- `GET /api/v1/attendance/summary`: Real-time overall percentage, total conducted, attended, safe skips, and recovery counts.
- `GET /api/v1/attendance/subjects`: Subject breakdown cards with individual status badges.
- `GET /api/v1/attendance/history`: Lecture-by-lecture audit history.
- `POST /api/v1/attendance/od-leave`: Submit digital OD leave application with proof upload.

### 6.4 Admin & Reports
- `POST /api/v1/admin/roster/upload`: Multipart Master Division Excel Roster import.
- `POST /api/v1/admin/allocations`: Assign faculty to subjects and lab batches.
- `GET /api/v1/reports/defaulters?division=D15B&threshold=75.0&format=json|xlsx|pdf`: Generates monthly defaulter lists and printable A4 notices.