-- Flyway V3: ERP Core Schema - Allocations, Enrollments, Lecture Sessions, and Absentees Marking

-- 1. Ensure sync_logs has source_identifier and decouple from worksheet_mappings
ALTER TABLE sync_logs ADD COLUMN IF NOT EXISTS source_identifier VARCHAR(255);
ALTER TABLE sync_logs DROP CONSTRAINT IF EXISTS sync_logs_worksheet_mapping_id_fkey;
DROP TABLE IF EXISTS worksheet_mappings CASCADE;

-- 2. Faculty Subject Allocations
CREATE TABLE IF NOT EXISTS faculty_subject_allocations (
    id VARCHAR(36) PRIMARY KEY,
    faculty_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id VARCHAR(36) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    division VARCHAR(32) NOT NULL,
    batch VARCHAR(32),
    academic_year VARCHAR(32) NOT NULL DEFAULT '2025-26',
    semester INT NOT NULL DEFAULT 6,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_faculty_subject_alloc UNIQUE (faculty_id, subject_id, division, batch, academic_year, semester)
);

-- 3. Student Subject Enrollments (supports Open Electives & Division Cohorts)
CREATE TABLE IF NOT EXISTS student_subject_enrollments (
    id VARCHAR(64) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    subject_id VARCHAR(36) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    academic_year VARCHAR(32) NOT NULL DEFAULT '2025-26',
    semester INT NOT NULL DEFAULT 6,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_subject_enroll UNIQUE (student_id, subject_id, academic_year, semester)
);

-- 4. Lecture Sessions (Atomic class attendance instance submitted by faculty)
CREATE TABLE IF NOT EXISTS lecture_sessions (
    id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(36) NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    faculty_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    division VARCHAR(32) NOT NULL,
    batch VARCHAR(32),
    session_date DATE NOT NULL,
    session_index INT NOT NULL DEFAULT 1,
    session_type VARCHAR(32) NOT NULL DEFAULT 'LECTURE',
    topic_covered VARCHAR(500),
    total_students INT NOT NULL DEFAULT 0,
    present_count INT NOT NULL DEFAULT 0,
    absent_count INT NOT NULL DEFAULT 0,
    payload_hash VARCHAR(64),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correction_reason TEXT,
    CONSTRAINT uq_lecture_session UNIQUE (subject_id, division, batch, session_date, session_index)
);

-- 5. Link Attendance Records to Lecture Sessions
ALTER TABLE attendance_records ADD COLUMN IF NOT EXISTS lecture_session_id VARCHAR(36) REFERENCES lecture_sessions(id) ON DELETE SET NULL;
ALTER TABLE attendance_records ALTER COLUMN source_row_hash DROP NOT NULL;

-- 6. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_faculty_alloc_faculty ON faculty_subject_allocations(faculty_id);
CREATE INDEX IF NOT EXISTS idx_faculty_alloc_subject ON faculty_subject_allocations(subject_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_student ON student_subject_enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_subject ON student_subject_enrollments(subject_id);
CREATE INDEX IF NOT EXISTS idx_sessions_subject ON lecture_sessions(subject_id);
CREATE INDEX IF NOT EXISTS idx_sessions_faculty ON lecture_sessions(faculty_id);
CREATE INDEX IF NOT EXISTS idx_sessions_date ON lecture_sessions(session_date);
CREATE INDEX IF NOT EXISTS idx_attendance_session ON attendance_records(lecture_session_id);

-- 7. Seed Faculty & Class Teacher Accounts and Default Subject Allocations
INSERT INTO users (id, email, password_hash, role, is_active) VALUES
  ('usr_fac_01', 'faculty@ves.ac.in', '$2a$10$7R0ZqIkmN1oT1B2gP3sOseW3VlO0/F0aP1yO2zO3qO4rO5sO6tO7u', 'FACULTY', true),
  ('usr_ct_01', 'classteacher@ves.ac.in', '$2a$10$7R0ZqIkmN1oT1B2gP3sOseW3VlO0/F0aP1yO2zO3qO4rO5sO6tO7u', 'CLASS_TEACHER', true)
ON CONFLICT (email) DO UPDATE SET role = EXCLUDED.role;

-- Ensure standard subjects exist
INSERT INTO subjects (id, code, name, total_planned) VALUES
  ('sub_cs401', 'CS401', 'Data Structures & Algorithms', 45),
  ('sub_cs402', 'CS402', 'Database Management Systems', 45),
  ('sub_cs403', 'CS403', 'Operating Systems', 45),
  ('sub_cs404', 'CS404', 'Computer Networks', 40),
  ('sub_cs405', 'CS405', 'Cloud Computing (Elective)', 36)
ON CONFLICT (code) DO NOTHING;

-- Allocate subjects to Faculty & Class Teacher for division D12B
INSERT INTO faculty_subject_allocations (id, faculty_id, subject_id, division, batch, academic_year, semester) VALUES
  ('alloc_01', 'usr_fac_01', 'sub_cs401', 'D12B', NULL, '2025-26', 6),
  ('alloc_02', 'usr_fac_01', 'sub_cs402', 'D12B', NULL, '2025-26', 6),
  ('alloc_03', 'usr_ct_01', 'sub_cs403', 'D12B', NULL, '2025-26', 6),
  ('alloc_04', 'usr_ct_01', 'sub_cs404', 'D12B', NULL, '2025-26', 6)
ON CONFLICT DO NOTHING;

-- Enroll seeded D12B students in the core subjects
INSERT INTO student_subject_enrollments (id, student_id, subject_id, academic_year, semester)
SELECT 
  MD5(CONCAT('enr_', s.id, '_', sub.id)),
  s.id,
  sub.id,
  '2025-26',
  6
FROM students s
CROSS JOIN (SELECT id FROM subjects WHERE code IN ('CS401', 'CS402', 'CS403', 'CS404')) sub
ON CONFLICT DO NOTHING;
