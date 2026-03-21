# Backend API (`api`)

## Tech Stack
- Java 21
- Spring Boot 3.4.x
- Spring Web
- Jakarta Validation
- Spring Data JPA
- MariaDB

## Architecture
- `controller`: REST API entry points.
- `service`: Domain/service layer (`TutorDataService`) mapped to repository queries.
- `entity`: JPA entities persisted in MariaDB.
- `repository`: Spring Data JPA repositories.
- `config`: startup data seeding (`DemoDataSeeder`).
- `model`: Request/response records with JavaDoc and validation annotations.

## Domain Relations
- One `Project` has many `Students`.
- One `Project` has many `Timeslots` (calendar items).
- One `Project` has many monthly `Reports`.
- A monthly report is generated from that project's timeslots for a selected month (`yyyy-MM`).

## Endpoints

### `GET /api/health`
Returns operational status.

### `GET /api/projects`
Returns project cards for the dashboard.

### `POST /api/projects`
Creates a project.

### `GET /api/projects/{projectId}/calendar`
Returns the active calendar payload and month slots.

Optional query: `month=yyyy-MM`.

### `GET /api/projects/{projectId}/students`
Returns student cards for the selected project.

### `POST /api/projects/{projectId}/students`
Creates a student in the selected project.

Request example:
```json
{
  "name": "Alex Thompson",
  "lastActive": "just now",
  "notes": "Needs additional practice on quadratics.",
  "groupName": "Group A"
}
```

### `GET /api/projects/{projectId}/timeslots`
Returns project timeslots for a selected month (`month=yyyy-MM`, optional).

### `POST /api/projects/{projectId}/timeslots`
Creates a timeslot in the selected project's calendar.

Request example:
```json
{
  "title": "Integration Workshop",
  "description": "Focused revision session",
  "durationMinutes": 90,
  "date": "2026-03-20",
  "startTime": "14:00"
}
```

### `GET /api/projects/{projectId}/timeslots/{timeslotId}`
Returns one timeslot by id within a project scope.

### `PUT /api/projects/{projectId}/timeslots/{timeslotId}`
Updates one timeslot in a project calendar.

### `DELETE /api/projects/{projectId}/timeslots/{timeslotId}`
Deletes one timeslot and returns `204 No Content`.

### `PATCH /api/students/{studentId}/notes`
Updates the notes field for a single student.

### `PATCH /api/students/{studentId}/group`
Assigns or reassigns a student to a project group.

Request example:
```json
{
  "groupName": "Group B"
}
```

If the project does not exist, the API returns `404`:
```json
{
  "timestamp": "2026-03-20T15:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found: unknown-slug",
  "path": "/api/projects/unknown-slug/students"
}
```

### `GET /api/reports`
Returns monthly report rows for export tables.

### `GET /api/projects/{projectId}/reports`
Returns monthly reports for one project.

### `POST /api/projects/{projectId}/reports/generate?month=yyyy-MM`
Generates or refreshes one monthly report from the selected project's timeslots.

### `POST /api/timeslots` (legacy compatibility)
Creates a new timeslot through the non-project-scoped route.

Request example:
```json
{
  "title": "Integration Workshop",
  "description": "Focused revision session",
  "durationMinutes": 90,
  "date": "2026-03-20",
  "startTime": "14:00"
}
```

## Error Contract
Validation and not-found errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-03-20T15:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Timeslot not found: unknown-id",
  "path": "/api/projects/math-grade-10/timeslots/unknown-id"
}
```

## Run
```bash
docker compose up -d
mvn spring-boot:run
```

If needed, override connection values:
```bash
set DB_URL=jdbc:mariadb://localhost:3307/tutortimetracker
set DB_USERNAME=tutortime
set DB_PASSWORD=tutortime
```

Note: Docker maps MariaDB to host port `3307` by default to avoid collisions with local MariaDB services on `3306`.

## CORS
CORS is enabled for `http://localhost:5173` in `TutorDataController`.
