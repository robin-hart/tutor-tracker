# Frontend (`frontend`)

## Tech Stack
- Vue 3 (Composition API)
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
- `src/services/apiClient.js`: Typed-by-contract API call functions.
- `src/router/index.js`: Central route and navigation definitions.

## Data Flow
1. Views load and call service functions from `apiClient.js`.
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
```
