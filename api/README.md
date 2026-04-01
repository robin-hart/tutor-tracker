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

### `GET /api/projects/{projectId}/reports/export/pdf?month=yyyy-MM`
Builds a LaTeX document from the selected project's timeslots in the month and returns a PDF
download.

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

Optional LaTeX export settings:
```bash
set LATEX_COMMAND=pdflatex
set LATEX_TIMEOUT_SECONDS=30
set LATEX_TEMPLATE=templates/project-monthly-report.tpl
```

Install a LaTeX distribution (for example TeX Live or MiKTeX) so `pdflatex` is available on your
PATH when using PDF export.

Note: Docker maps MariaDB to host port `3307` by default to avoid collisions with local MariaDB services on `3306`.

## Quality & Testing

### Code Quality & Formatting

Backend code quality is enforced during the build via Checkstyle (style rules) and Spotless (auto-formatting):

**Check violations:**
```bash
mvn checkstyle:check
mvn spotless:check
```

**Auto-format code:**
```bash
mvn spotless:apply
```

**Configuration:**
- Checkstyle max line length: `100` characters
- Formatter: google-java-format (opinionated, minimal config)
- Quality gate runs during: `mvn verify`

### Testing & Coverage

The backend test setup is isolated from Docker:
- **Unit tests** use Mockito (`*Test.java`).
- **Integration tests** use Spring Boot + MockMvc + in-memory H2 (`*IT.java`).
- `test` profile disables demo seeders and legacy DB cleanup for deterministic tests.

**Run tests:**
```bash
mvn test                    # Unit tests only
mvn verify                  # Unit + integration + coverage checks
```

**Coverage:**
- JaCoCo collects data during tests and generates reports at `target/site/jacoco/index.html`
- Minimum thresholds: 30% line coverage, 20% branch coverage
- Thresholds are enforced during `mvn verify`

## CORS
CORS is enabled for `http://localhost:5173` in `TutorDataController`.

## Documentation

### API Reference

**Scalar UI** (interactive OpenAPI documentation):
```bash
mvn spring-boot:run
# Open: http://localhost:8080/scalar/index.html
```

Also available at: `http://localhost:8080/v3/api-docs` (OpenAPI JSON)

### Javadoc

Generate full API documentation:
```bash
mvn javadoc:javadoc
# View: target/site/apidocs/index.html
```

**Includes:**
- All public classes and methods across layers (Controllers, Services, Models, Repositories)
- Parameter and return value documentation
- Architecture overview with entity relationships
- Business rules and validation constraints

**Excluded:** Internal configuration package (`com.tutortimetracker.api.config`)

**Skip during build:**
```bash
mvn package -DskipJavadoc
```
