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

### 1) Local Installation Mode

Start your local MariaDB installation on the default port `3306`.

Before starting the backend, copy [.env.example](.env.example) to [.env](.env) in the repository
root and keep the database credentials there. The backend development profile reads that file on
startup.

Start backend:

```bash
cd api
mvn spring-boot:run
```

The backend defaults to the `development` profile in local mode.
In this profile, JPA runs with `create-drop`, so the schema is recreated on every start.

Start frontend:

```bash
cd frontend
npm install
npm run dev
```

Local mode requirements:
- Install LaTeX locally and ensure `pdflatex` is available in your PATH.
- The backend always uses a local LaTeX command (`LATEX_COMMAND`, default `pdflatex`).

### 2) Full Docker Mode (Alternative Runtime)

Run the whole stack (MariaDB + backend + frontend via nginx):

```bash
docker compose up --build
```

Docker mode endpoints:
- Frontend (nginx): `http://localhost:5173` by default
- Backend API: `http://localhost:8080/api`

To change the published frontend port, edit `FRONTEND_HOST_PORT` in the repository root
[.env](.env) file.

In Docker mode, MariaDB is internal to the Docker network and is not exposed on a host port.

In Docker mode, LaTeX is installed inside the backend container and is used there as a local
compiler by the backend process.

Vue single-file components can be formatted with Prettier using:

```bash
npm run format:vue
```

Run strict frontend formatting and typing checks with:

```bash
npm run format:check:strict
npm run typecheck
```

In local installation mode, the frontend runs at `http://localhost:5173` and calls
`http://localhost:8080/api`.

### Stage Behavior

- Local backend runs with `development` by default (`SPRING_PROFILES_ACTIVE` not set).
- Docker backend always runs with `production` (`SPRING_PROFILES_ACTIVE=production`).
- In `production`, schema changes are managed by Flyway migrations and Hibernate runs in
  `validate` mode, so existing database rows are not dropped on startup.

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
