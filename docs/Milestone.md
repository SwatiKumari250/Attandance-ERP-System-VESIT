# OpenAttend — Milestones & Roadmap (Native Attendance ERP)

> Derived from `PRD.md`, `architecture.md`, and GitHub Roadmap Issues (#1 through #16).  
> Purpose: Sequenced build plan for autonomous development. Each phase is independently testable, idempotent, and production-grade.

---

## 🗺️ High-Level Milestone Phases

```
┌──────────────────────────────────────────────────────────┐
│ Phase 1: Semester Onboarding & Contributor Setup        │
│ (Excel Roster Upload, Faculty Allocation, Docker Dev)    │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│ Phase 2: High-Speed Attendance Marking & Correction      │
│ (Absentees UI, Fast Submit API, 48h Audit Lock, PWA Sync)│
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│ Phase 3: Defaulter Reporting & Academic Diary            │
│ (1-Click Defaulter Engine, A4 PDF Notice, NAAC Diary)    │
└────────────────────────────┬─────────────────────────────┘
                             │
┌────────────────────────────▼─────────────────────────────┐
│ Phase 4: Student Experience & Advanced ERP Workflows     │
│ (Dashboard Rings, Safe Skips, OD Portal, Proxy Lectures) │
└──────────────────────────────────────────────────────────┘
```

---

## 🚀 Phase 1: Semester Onboarding & Contributor Setup

### Issue #1: Master Semester Excel Roster Upload & Student-Elective Parser
- **Scope:**
  - Endpoint `POST /api/v1/admin/roster/upload` accepting multipart `.xlsx` / `.csv`.
  - Apache POI parser reading: Roll No, Name, Email (`@ves.ac.in`), Division (`D15B`), Lab Batch (`A`, `B`, `C`), and Elective Course.
  - Upserts `users` (password defaulted to email), `students`, and `student_subject_enrollments`.
  - Returns JSON summary: `{ "totalUploaded": 71, "newStudents": 3, "updated": 68, "errors": [] }`.
- **Exit Criteria / DoD:**
  - Successfully parses division spreadsheet with zero data corruption.
  - Re-uploading the same file is 100% idempotent.
  - Rejects non-`@ves.ac.in` emails with itemized row-level validation errors.

### Issue #2: Faculty-to-Subject & Batch Allocation API with Access Control
- **Scope:**
  - Table `faculty_subject_allocations` (`faculty_id`, `subject_id`, `division`, `batch`, `semester`).
  - Endpoint `POST /api/v1/admin/allocations` (Admin assigns classes to teachers).
  - Endpoint `GET /api/v1/faculty/my-subjects` (Returns assigned classes for authenticated faculty).
  - Security Pre-Authorization: Teachers can only view rosters and mark attendance for classes assigned to them (`403 Forbidden` on unauthorized attempts).
- **Exit Criteria / DoD:**
  - `GET /api/v1/faculty/my-subjects` returns strictly the logged-in teacher's classes.
  - Integration tests verify RBAC boundary enforcement.

### Issue #15 & #16: One-Command Docker Compose & Sample Excel Seeding
- **Scope:**
  - `docker-compose.yml` pre-configuring PostgreSQL 16 (`openattend_dev`).
  - Sample seed Excel file `seeds/sample_division_D15B.xlsx` with ~70 realistic student records.
- **Exit Criteria / DoD:**
  - Developer can run `docker compose up -d` and launch the system cleanly.

---

## ⚡ Phase 2: High-Speed Attendance Marking & Correction

### Issue #3: High-Speed Attendance Marking UI ("Mark Absentees" Mode)
- **Scope:**
  - Selection header: Date picker, Subject & Batch dropdown (populated from `/my-subjects`), Slot.
  - Interactive grid displaying student chips (default: **Present**).
  - Tapping a chip toggles state to **Absent** (vibrant red badge).
  - Live summary counter: `Total: 71 | Present: 67 | Absent: 4 (Roll: 3, 6, 8, 11)`.
  - Optional `Topic Covered` field and sticky `Submit Attendance` button.
- **Exit Criteria / DoD:**
  - Submits only the list of absent roll numbers to the backend API.
  - Fast mobile tapping response (<50ms touch lag).

### Issue #4: Fast Attendance Submission API with Idempotency & SHA-256 Hashing
- **Scope:**
  - Endpoint `POST /api/v1/faculty/attendance/submit`.
  - Atomically records `LectureSession` and marks enrolled absentees as `ABSENT` and remainder as `PRESENT`.
  - Keyed by natural key `(subject_id, division, batch, lecture_date, session_index)`.
  - Computes SHA-256 session hash to prevent duplicate submissions on double-clicks.
- **Exit Criteria / DoD:**
  - Submits in `<100ms` for a class of 80 students.
  - Zero duplicate rows created on repeated taps.

### Issue #5: 48-Hour Attendance Correction Window with Mandatory Audit Reason
- **Scope:**
  - Endpoint `PUT /api/v1/faculty/attendance/{sessionId}/correct`.
  - Allows subject teachers to edit marking within 48 hours.
  - Requires non-empty reason (`@NotBlank`, min length 10 characters).
  - Creates immutable change log in `attendance_history_events`.
  - Automatically locks session after 48 hours (`403 Forbidden` thereafter).
- **Exit Criteria / DoD:**
  - Faculty blocked from editing sessions older than 48 hours.
  - Audit event persists caller email, timestamp, and explanation.

### Issue #14: Offline Classroom Attendance Caching with Auto-Sync (PWA)
- **Scope:**
  - Service Worker caches shell and today's rosters in IndexedDB.
  - If offline during submit, saves payload to `pending_submissions` and auto-submits on `window.online`.
- **Exit Criteria / DoD:**
  - Marking UI functions 100% offline in basement labs.

---

## 📊 Phase 3: Defaulter Reporting & Academic Diary

### Issue #6: 1-Click Defaulter List Generator (Excel/CSV Export with Date Filters)
- **Scope:**
  - Endpoint `GET /api/v1/reports/defaulters?division=D15B&threshold=75.0&format=json|xlsx|csv`.
  - Fast SQL calculation aggregating conducted vs attended lectures.
  - Exports Roll No, Name, Total Conducted, Total Attended, Overall %, and individual subject columns.
- **Exit Criteria / DoD:**
  - Accurate boundary math (`74.9%` is defaulter, `75.0%` is safe).
  - Handles elective course denominations correctly.

### Issue #7: Official Notice-Board Ready Defaulter PDF Generator
- **Scope:**
  - Printable A4 PDF layout with VESIT institutional letterhead:
    - 🔴 Critical Defaulters (`< 65%`)
    - 🟡 Warning Zone (`65% – 74.9%`)
    - 🟢 Safe Zone (`≥ 75%`)
  - Formal signature blocks for Class Teacher, Attendance Coordinator, and HOD.
- **Exit Criteria / DoD:**
  - Formatted cleanly to fit standard A4 paper without awkward table breaks.

### Issue #8: NAAC / NBA Academic Course Diary & Lecture Log ("Topics Covered" Tracker)
- **Scope:**
  - Extends lecture sessions to store `topic_covered`, `lecture_type` (`THEORY`, `LAB`, `TUTORIAL`), and headcounts.
  - Endpoint `GET /api/v1/faculty/diary?subjectId=...` returning chronological syllabus metrics.
- **Exit Criteria / DoD:**
  - Course diary syllabus metrics exported cleanly for NAAC Course Files.

---

## 🎓 Phase 4: Student Experience & Advanced ERP Workflows

### Issue #9: Mobile-First Student Dashboard with Percentage Rings & Status Badges
- **Scope:**
  - Animated SVG circular percentage ring (Green $\ge 75\%$, Yellow $65\% - 74.9\%$, Red $< 65\%$).
  - Subject breakdown cards with Safe / Defaulter badges.
  - Date-by-date lecture history modal.
- **Exit Criteria / DoD:**
  - Instant load (<100ms) with zero layout shift on mobile screens.

### Issue #10: Safe Skips & Attendance Recovery Target Calculator
- **Scope:**
  - Safe Skips: $S = \lfloor \frac{4A - 3T}{3} \rfloor$
  - Must-Attend: $R = \lceil 3T - 4A \rceil$
  - Standalone service with 100% boundary test coverage.

### Issue #11: Automated 75% Attendance Defaulter Warning Notifications
- **Scope:**
  - Detects drop below 75% upon lecture submission.
  - Sends email to student's `@ves.ac.in` with 7-day cooldown throttling.

### Issue #12: Paperless Official Duty (OD) & Medical Leave Approval Portal
- **Scope:**
  - Student applies with event name, dates, and proof upload.
  - Teacher/Coordinator approves with 1-click.
  - Approved leaves automatically recalculate attendance as `DUTY_PRESENT`.

### Issue #13: Proxy / Substitute Lecture Delegation with Audit Trail
- **Scope:**
  - `POST /api/v1/faculty/proxy/assign` allows delegating a lecture slot to a colleague with audit tracking (`is_proxy = true`).

---

## 🛡️ Non-Negotiable Invariants

1. **High-Speed Marking UX**: Entire division marked in <10 seconds by tapping only absentees.
2. **Strict Idempotency**: Natural key + SHA-256 session hash prevents duplicate writes.
3. **48-Hour Audit Lock**: Edits within 48 hours require 10+ char justification; locked thereafter.
4. **Institutional Email Validation**: Non-`@ves.ac.in` accounts cannot authenticate.
5. **No Google Sheets Dependency**: Attendance is created and stored natively in PostgreSQL.