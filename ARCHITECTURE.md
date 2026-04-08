# TutorTimeTracker Architecture (Human + AI Readable)

## 1. Context
The project transforms static UI mockups into a navigable SPA and a backing REST API.

## 2. Logical Components
- Frontend SPA
  - View rendering, route transitions, API consumption, and action feedback (loading/errors/success states).
- Backend API
  - MariaDB-backed query responses, validation, and project-scoped write operations.

## 2.1 Core Data Flow
- Projects are top-level containers.
- Students are attached to one project.
- Timeslots are attached to one project calendar.
- Monthly reports are generated per project from timeslots in a selected month.
- Calendar interactions are date-driven: selecting a day filters visible timeslots and anchors timeslot creation to that date.

## 2.2 Runtime Modes
- Local installation mode:
  - Frontend runs with Vite.
  - Backend runs with Spring Boot.
  - MariaDB is typically started via Docker.
  - PDF export uses a locally installed LaTeX command (`pdflatex` by default).
- Full Docker mode:
  - A root-level Docker Compose stack runs MariaDB, backend, and frontend.
  - Frontend is served by nginx and proxies `/api` to the backend service.
  - LaTeX is installed in the backend container, so the backend still uses a local LaTeX command within that container.

## 3. Frontend Design Decisions
- Preserved visual language using Tailwind token palette from provided designs.
- Implemented shared shell components (`AppSidebar`, `MainTopBar`) for consistency.
- Kept each mockup as a dedicated view for easy one-to-one design traceability.

## 4. Backend Design Decisions
- Used Java records for concise, immutable API contracts.
- Added JavaDoc at class and method level for maintainability.
- Centralized persistence mapping in `TutorDataService` to keep controllers thin.
- Seeded startup data in `DemoDataSeeder` for immediate mockup parity.

## 4.1 Database Evolution Model
The backend uses two runtime stages for database behavior:

- `development`
  - Local-first workflow for active implementation.
  - Hibernate uses `create-drop` so the schema is recreated on each startup.
  - Demo data seeding and legacy cleanup helpers are allowed.
- `production`
  - Safe runtime for Docker and later real deployments.
  - Hibernate uses `validate` so no tables are dropped or recreated on startup.
  - Flyway applies versioned migrations from `src/main/resources/db/migration`.

Database evolution rules:

- Never edit a migration file after it has been applied in any shared environment.
- Add a new migration for every schema change.
- Prefer expand-contract changes for breaking schema work.
  - Expand: add new columns, tables, or constraints in a backward-compatible way.
  - Backfill: migrate data into the new structure.
  - Contract: remove legacy columns or code only after the new path is deployed and stable.
- Treat destructive schema changes as a later release step, not as part of the first code change.

Migration naming convention:

- `V2__add_student_email.sql`
- `V3__backfill_student_email.sql`
- `V4__drop_legacy_student_notes.sql`

Validation expectations:

- Fresh database startup must succeed from migration version 1 to the latest version.
- Upgrade startup must succeed from the previous release schema to the current release schema.
- Production Docker startup must run with `production` only.

## 5. Integration Contract
Frontend service module `src/services/apiClient.js` maps one function per endpoint.

| Frontend Function | HTTP Endpoint | Purpose |
|---|---|---|
| `getProjects()` | `GET /api/projects` | Dashboard cards |
| `createProject(payload)` | `POST /api/projects` | Create project card |
| `getProjectCalendar(projectId, month)` | `GET /api/projects/{id}/calendar?month=yyyy-MM` | Calendar view |
| `getProjectStudents(projectId)` | `GET /api/projects/{id}/students` | Students view |
| `createProjectStudent(projectId, payload)` | `POST /api/projects/{id}/students` | Add student |
| `updateStudentNotes(studentId, notes)` | `PATCH /api/students/{id}/notes` | Save student notes |
| `getReports()` | `GET /api/reports` | Reports table |
| `getProjectReports(projectId)` | `GET /api/projects/{id}/reports` | Project reports list |
| `generateProjectReport(projectId, month)` | `POST /api/projects/{id}/reports/generate` | Monthly report generation |
| `exportProjectReportPdf(projectId, month)` | `GET /api/projects/{id}/reports/export/pdf?month=yyyy-MM` | Monthly PDF export |
| `saveTimeslot(projectId, payload)` | `POST /api/projects/{id}/timeslots` | Create timeslot |
| `getProjectTimeslot(projectId, timeslotId)` | `GET /api/projects/{id}/timeslots/{timeslotId}` | Load timeslot for edit |
| `updateProjectTimeslot(projectId, timeslotId, payload)` | `PUT /api/projects/{id}/timeslots/{timeslotId}` | Update timeslot |
| `deleteProjectTimeslot(projectId, timeslotId)` | `DELETE /api/projects/{id}/timeslots/{timeslotId}` | Delete timeslot |

## 6. Future-Ready Extension Points
- Add authentication and project ownership checks.
- Introduce OpenAPI generation and contract tests.
- Add component-level visual regression tests.
- Add dedicated migration upgrade tests for production schema evolution.
