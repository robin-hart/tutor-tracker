# TutorTimeTracker

TutorTimeTracker is a full-stack implementation of the provided mockups using:
- `frontend/`: Vue 3 + TypeScript + Vite + Vue Router + Tailwind CSS
- `api/`: Java 21 + Spring Boot REST API + MariaDB

## Goals Implemented
- Translated the provided mockup screens into routed Vue views.
- Connected pages logically with route navigation and shared app shell components.
- Added backend endpoints in `api` to serve project-scoped dashboard, calendar, student, report, and timeslot CRUD flows.
- Added thorough in-code documentation:
  - JavaDoc for backend classes, records, methods.
  - JSDoc-style comments for frontend modules.
- Added human + AI-readable architecture docs and endpoint contracts.

## Mockup Mapping
- `projects_dashboard/code.html` -> `frontend/src/views/ProjectsDashboardView.vue`
- `project_calendar_view_fixed/code.html` -> `frontend/src/views/ProjectCalendarView.vue`
- `student_management/code.html` -> `frontend/src/views/StudentManagementView.vue`
- `timeslot_editor/code.html` -> `frontend/src/views/TimeslotEditorView.vue`
- `export_reports/code.html` -> `frontend/src/views/ExportReportsView.vue`

## Run Instructions

### 1) Backend
```bash
cd api
docker compose up -d
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.
MariaDB is exposed on host port `3307` by default.

### 2) Frontend
```bash
cd frontend
npm install
npm run dev
```

Vue single-file components can be formatted with Prettier using:

```bash
npm run format:vue
```

Run strict frontend formatting and typing checks with:

```bash
npm run format:check:strict
npm run typecheck
```

The frontend runs at `http://localhost:5173` and calls `http://localhost:8080/api`.

## API Surface (Summary)
- `GET /api/health`
- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{projectId}/calendar`
- `GET /api/projects/{projectId}/students`
- `POST /api/projects/{projectId}/students`
- `PATCH /api/students/{studentId}/notes`
- `GET /api/projects/{projectId}/timeslots?month=yyyy-MM`
- `POST /api/projects/{projectId}/timeslots`
- `GET /api/projects/{projectId}/timeslots/{timeslotId}`
- `PUT /api/projects/{projectId}/timeslots/{timeslotId}`
- `DELETE /api/projects/{projectId}/timeslots/{timeslotId}`
- `GET /api/reports`
- `GET /api/projects/{projectId}/reports`
- `POST /api/projects/{projectId}/reports/generate?month=yyyy-MM`
- `GET /api/projects/{projectId}/reports/export/pdf?month=yyyy-MM`

Legacy compatibility endpoint still available:
- `POST /api/timeslots`

## Notes
- Data is persisted in MariaDB through Spring Data JPA.
- On first boot, `DemoDataSeeder` inserts baseline records to match the mockups.
- The relation model is project-centric: students, calendar timeslots, and monthly reports are all scoped to one project.
