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

## 3. Frontend Design Decisions
- Preserved visual language using Tailwind token palette from provided designs.
- Implemented shared shell components (`AppSidebar`, `MainTopBar`) for consistency.
- Kept each mockup as a dedicated view for easy one-to-one design traceability.

## 4. Backend Design Decisions
- Used Java records for concise, immutable API contracts.
- Added JavaDoc at class and method level for maintainability.
- Centralized persistence mapping in `TutorDataService` to keep controllers thin.
- Seeded startup data in `DemoDataSeeder` for immediate mockup parity.

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
