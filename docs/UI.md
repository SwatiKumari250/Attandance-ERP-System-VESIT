# OpenAttend — UI Build Specification (Native Attendance ERP)

> Derived from `PRD.md` (v2.0) and `architecture.md`  
> Stack: Vanilla HTML5 / Modern CSS (Design Tokens) / Vanilla JavaScript / PWA Service Worker.

---

## 0. Design Direction & Principles

OpenAttend is a high-speed, modern **Native Attendance ERP System**. The user experience prioritizes:
1. **Ultra-Fast Classroom Marking (<10 Seconds):** Faculty should never spend more than 10 seconds recording attendance for 70+ students.
2. **Instant Student Status Clarity:** Every student dashboard screen must answer *"Am I above 75%?"* within two seconds.
3. **Calm, High-Contrast Aesthetics:** Clean typography, glassmorphism, responsive mobile layouts, and polished Light/Dark themes.

### Design Tokens

```css
/* Light (default notebook theme) */
--bg-base:        #FAFAF8;
--bg-surface:     #FFFFFF;
--bg-surface-2:   #F2F1EC;
--border-subtle:  #E7E5DE;
--border-strong:  #D3D0C6;

--text-primary:   #1C1B18;
--text-secondary: #6B6A62;
--text-tertiary:  #A3A199;

--accent:         #2F6F5E;   /* OpenAttend Forest Pine */
--accent-dim:     #E4EFEB;

--state-safe:     #2F9E6E;   /* ≥ 75%, Present */
--state-risk:     #D64545;   /* < 65%, Absent, Critical Defaulter */
--state-warn:     #C98A1E;   /* 65% – 74.9%, Warning Zone */
--state-neutral:  #8B897F;   /* No lecture / NA */
```

```css
/* Dark (Night study theme) */
--bg-base:        #14161A;
--bg-surface:     #1B1E23;
--bg-surface-2:   #22262C;
--border-subtle:  #2B3038;
--border-strong:  #3A424C;

--text-primary:   #ECEBE6;
--text-secondary: #9B9A92;
--text-tertiary:  #6B6A62;

--accent:         #5FBFA6;
--accent-dim:     #1E2E29;

--state-safe:     #4ADE94;
--state-risk:     #F27272;
--state-warn:     #E0AA46;
--state-neutral:  #6E7178;
```

---

## 1. Global Shell & Navigation

- **Sidebar Header:** Official OpenAttend icon badge (`assets/icon.png`), institutional title, and subtitle (`VESIT Attendance`).
- **Role Switcher Footer:** Dynamically swaps between `STUDENT`, `FACULTY`, and `ADMIN` navigation suites based on authenticated credentials.

### Navigation Groups

#### 🎓 Student Navigation
- `📊 Dashboard` — Percentage Ring, quick stats, subject summary.
- `📈 Analytics` — Weekly attendance trajectories and trends.
- `⚡ Predictor` — Safe Skips & Must-Attend calculation engine.
- `📜 History` — Date-by-date lecture log and CSV export.
- `🔔 Notifications` — 75% threshold breach warnings.
- `📝 OD Leave` — Paperless Official Duty & Medical leave application portal.

#### 👨‍🏫 Faculty Navigation
- `⚡ Mark Attendance` — High-Speed "Mark Absentees" chip grid (<10s).
- `📚 My Classes` — Allocated subjects, divisions, and lab batches.
- `📖 Academic Diary` — NAAC syllabus coverage & lecture log.
- `🔄 Proxy Delegation` — Temporary substitute teacher assignment.

#### ⚙️ Admin & Class Teacher Navigation
- `📤 Master Roster Upload` — Start-of-semester division Excel spreadsheet import.
- `👥 Faculty Allocations` — Assign teachers to subjects and lab batches.
- `📄 Defaulter Notices` — 1-Click calculation and printable A4 notice PDF.
- `📋 Audit Logs` — Immutable attendance history events and corrections.

---

## 2. Faculty High-Speed Marking UI ("Mark Absentees" Mode)

```
┌─────────────────────────────────────────────────────────────┐
│ Date: [ 2026-09-07 ]   Subject: [ OS - D15B ]   Batch: [ALL]│
├─────────────────────────────────────────────────────────────┤
│ [01] M. Ahuja     [02] K. Amarnani   [03] J. Arora ✕ (ABSENT)│
│ [04] S. Awasthi   [05] U. Bhangale   [06] S. Chhatlani      │
│ [07] K. Devadiga  [08] A. Dubey ✕    [09] B. Fatnani        │
├─────────────────────────────────────────────────────────────┤
│ Counter: Total: 71 | Present: 69 | Absent: 2 (Roll: 3, 8)   │
│ Topic Covered: [ Process Synchronization & Semaphores     ] │
│                               [ ⚡ Submit Attendance ]      │
└─────────────────────────────────────────────────────────────┘
```

- **Interactive Chips Grid:**
  - Enrolled students display as numbered chips.
  - **Default state is Present** (neutral soft background).
  - Tapping a chip toggles state to **Absent** (vibrant red badge with cross `✕`).
- **Batch Filter Pills:** `All`, `Batch A`, `Batch B`, `Batch C` for theory vs lab practicals.
- **Action Bar:**
  - Live summary counter of present vs absent counts.
  - Optional `Topic Covered` input for NAAC diary.
  - Sticky `Submit Attendance` button submitting in `<100ms`.

---

## 3. 48-Hour Attendance Correction Modal

- Opened by tapping any past lecture session within 48 hours.
- Displays original submitter email and timestamp.
- Allows toggling student statuses (`PRESENT` ↔ `ABSENT` ↔ `DUTY_PRESENT`).
- **Mandatory Justification Input:** Requires minimum 10 characters explaining the correction (e.g. *"Student arrived during roll call after resolving lab equipment issue"*).
- Sessions older than 48 hours display a locked indicator: *"Locked after 48-hour academic integrity window"*.

---

## 4. Student Mobile Dashboard

- **Hero Percentage Ring:**
  - Animated SVG circular ring displaying overall percentage.
  - Dynamic color coding: Green ($\ge 75\%$), Yellow ($65\% - 74.9\%$), Red ($< 65\%$).
  - Mono-spaced center percentage (e.g., `82.4%`).
- **Predictor Callout:**
  - Safe Skips: *"You can safely skip 3 lectures while maintaining 75%"*.
  - Recovery Target: *"Must attend 5 consecutive lectures to regain 75%"*.
- **Subject Breakdown Cards:**
  - Left border colored by threshold zone.
  - Attended / Conducted count, individual percentage, and safe skips count.
  - Tap card to open lecture history modal.

---

## 5. Official Defaulter Notice PDF Layout

- **Paper Format:** Standard A4 printable layout.
- **Header:**
  ```
  VIVEKANAND EDUCATION SOCIETY'S INSTITUTE OF TECHNOLOGY (VESIT)
  Department of Information Technology
  Academic Year: 2026-27 | Semester: V (ODD)
  OFFICIAL ATTENDANCE DEFAULTER LIST (Period: 01-Aug-2026 to 31-Aug-2026)
  Class: D15 B
  ```
- **Categorized Bands:**
  - 🔴 **Critical Defaulters (`< 65%`)**
  - 🟡 **Warning Zone (`65% – 74.9%`)**
  - 🟢 **Safe Zone (`≥ 75%`)**
- **Footer Signatures:** Formal blocks for *Class Teacher*, *Attendance Coordinator*, and *Head of Department (HOD)*.

---

## 6. Master Semester Excel Roster Upload Screen

- Drag-and-drop file upload zone for `.xlsx` and `.csv`.
- Division selection (`D15B`, `D12B`), Semester, and Academic Year dropdowns.
- Pre-import validation preview table summarizing:
  - Total students detected
  - Lab batches detected (`A`, `B`, `C`)
  - Electives detected (`Cloud Computing`, `Soft Computing`, etc.)
  - Row validation errors (highlighting any invalid emails outside `@ves.ac.in`).
- Single-click **"Confirm & Import Roster"** executing atomic ingestion into PostgreSQL.