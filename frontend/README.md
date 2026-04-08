# Frontend (`frontend`)

## Tech Stack

- Vue 3 (Composition API)
- TypeScript (including Vue SFC script blocks)
- Vite
- Vue Router
- Tailwind CSS with design tokens taken from the mockups

## Route Map

- `/projects` -> Projects dashboard
- `/projects/:projectId/calendar` -> Project calendar
- `/projects/:projectId/students` -> Student management
- `/timeslots/edit` -> Timeslot editor modal page
- `/reports` -> Reports/export table

Timeslot editor query parameters:

- Create mode: `?projectId=<slug>&date=yyyy-MM-dd&month=yyyy-MM`
- Edit mode: `?projectId=<slug>&timeslotId=<id>&month=yyyy-MM`

## Structure

- `src/components/AppSidebar.vue`: Shared left navigation and main app links.
- `src/components/MainTopBar.vue`: Shared top navigation search/tabs shell.
- `src/views/*`: One view per provided mockup.
- `src/services/apiClient.ts`: Typed API call functions and payload contracts.
- `src/router/index.ts`: Central route and navigation definitions.

## Data Flow

1. Views load and call service functions from `apiClient.ts`.
2. Calendar day selection drives date-scoped timeslot listing and creation links.
3. Timeslot editor loads existing data when `timeslotId` is present and switches to update mode.
4. Delete actions call the API and remove the deleted slot from local view state.
5. Students are grouped by `groupName` and can be reassigned inline in the student management view.
6. API responses populate cards/tables; errors are surfaced as inline messages.

## Environment

Optional `.env`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

Recommended local setup:

1. Copy `.env.example` to `.env`.
2. Keep the default unless your backend runs on a different host or port.

If backend runs on a different port, set:

```bash
VITE_API_BASE_URL=http://localhost:<port>/api
```

## Commands

```bash
npm install
npm run dev
npm run build
npm run preview
npm run lint
npm run typecheck
npm run format:check
npm run format:check:strict
npm run format:vue
npm run format:vue:check
```

Format Vue files (single-file components) with Prettier:

```bash
npm run format:vue
npm run format:vue:check
```

## Docker Runtime

The full Docker setup is managed at repository root (`../docker-compose.yml`).

Run from the repository root:

```bash
docker compose up --build
```

In this mode:
- Frontend is served by nginx on `http://localhost:80` by default.
- `/api` requests are reverse-proxied by nginx to the backend container.

The published host port can be changed through `FRONTEND_HOST_PORT` in the repository root
`.env` file. The container still listens on port `80` internally.

## Testing

### Unit Tests

Run component and utility tests with Vitest:

```bash
npm run test          # Run in watch mode
npm run test:ui       # Open interactive UI
npm run test:run      # Run once and exit
```

### End-to-End Tests

First, install browser binaries for Playwright:

```bash
npx playwright install chromium
```

Run user workflow tests with Playwright (tests both frontend and backend):

```bash
npm run e2e              # Run all E2E tests (headless)
npm run e2e:ui           # Interactive UI - smoke tests only
npm run e2e:ui:all       # Interactive UI - all E2E tests
npm run e2e:debug        # Step-through debugging (headless)
npm run e2e:report       # View last test report
```

**E2E test suites:**

- `projects.e2e.ts` - Project CRUD and dashboard workflows
- `students.e2e.ts` - Student management and group assignments
- `timeslots.e2e.ts` - Calendar timeslot operations
- `reports.e2e.ts` - Report generation and data structure validation
- `ui-smoke.e2e.ts` - Quick smoke test for UI mode

**E2E tests verify:**

- Project creation and management workflows
- Student management across projects
- Calendar timeslot operations
- Report generation from timeslots
- API contracts and data structures
- Browser interactions and UI flows

**Note:** The interactive UI mode (`--ui`) runs in a single-threaded mode for better UX. Use `npm run e2e` for the fastest headless test runs with parallelization.

For detailed E2E testing guide, see: [E2E_TESTS.md](./E2E_TESTS.md)
