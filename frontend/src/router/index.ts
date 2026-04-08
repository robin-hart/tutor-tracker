import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import ExportReportsView from '../views/ExportReportsView.vue';
import ProjectCalendarView from '../views/ProjectCalendarView.vue';
import ProjectsDashboardView from '../views/ProjectsDashboardView.vue';
import StudentManagementView from '../views/StudentManagementView.vue';
import TimeslotEditorView from '../views/TimeslotEditorView.vue';

/**
 * Main route map for the TutorTimeTracker SPA.
 */
const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/projects' },
  { path: '/projects', name: 'projects-dashboard', component: ProjectsDashboardView },
  {
    path: '/projects/:projectId/calendar',
    name: 'project-calendar',
    component: ProjectCalendarView,
  },
  {
    path: '/projects/:projectId/students',
    name: 'student-management',
    component: StudentManagementView,
  },
  { path: '/timeslots/edit', name: 'timeslot-editor', component: TimeslotEditorView },
  { path: '/reports', name: 'export-reports', component: ExportReportsView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
