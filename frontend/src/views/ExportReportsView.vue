<template>
  <div class="text-on-surface bg-background min-h-screen">
    <AppSidebar />
    <MainTopBar search-placeholder="Search reports or sessions...">
      <template #tabs>
        <nav class="flex items-center gap-6">
          <a class="text-primary font-bold border-b-2 border-primary pb-1 font-manrope text-sm"
            >All Projects</a
          >
          <a class="text-on-surface-variant font-manrope text-sm">Active Sessions</a>
        </nav>
      </template>
    </MainTopBar>

    <main class="ml-72 pt-32 px-12 pb-20">
      <div class="mb-12">
        <h1 class="font-headline text-5xl font-extrabold tracking-tight mb-2">Reports & Exports</h1>
        <p class="text-on-surface-variant text-lg max-w-2xl">
          Analyze performance trends and generate professional documentation for your billing and
          session history.
        </p>
      </div>

      <section class="mb-8 grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
        <label class="block">
          <span class="text-xs text-on-surface-variant uppercase tracking-widest">Project</span>
          <select
            v-model="selectedProjectId"
            data-testid="report-project-select"
            class="mt-2 w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20"
          >
            <option v-for="project in projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </option>
          </select>
        </label>
        <div
          class="bg-surface-container-lowest rounded-xl px-4 py-3"
          data-testid="report-project-institution"
        >
          <p class="text-xs text-on-surface-variant uppercase tracking-widest">Institution</p>
          <p class="mt-2 font-semibold">{{ selectedProjectInstitution }}</p>
        </div>
        <div
          class="bg-surface-container-lowest rounded-xl px-4 py-3"
          data-testid="report-project-target"
        >
          <p class="text-xs text-on-surface-variant uppercase tracking-widest">
            Monthly Target Working Time
          </p>
          <p class="mt-2 font-semibold">{{ selectedProjectTargetLabel }}</p>
        </div>
      </section>

      <section class="mb-8 flex items-center gap-3">
        <button
          class="premium-gradient text-white px-4 py-2 rounded-lg font-bold text-sm"
          :disabled="!selectedProjectId || !newestMonthKey"
          @click="generateNewestMonthlyReport"
        >
          Generate Newest Monthly Report
        </button>
      </section>

      <div
        class="bg-surface-container-low rounded-2xl overflow-hidden shadow-[0_20px_40px_rgba(11,28,48,0.04)]"
      >
        <div
          class="p-8 flex items-center justify-between bg-surface-container-low border-b border-outline-variant/10"
        >
          <h2 class="font-headline font-bold text-2xl">Monthly Breakdowns</h2>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="text-on-surface-variant uppercase text-[10px] font-bold tracking-[0.2em]">
                <th class="px-8 py-6">Month / Period</th>
                <th class="px-8 py-6">Project Name</th>
                <th class="px-8 py-6">Total Hours</th>
                <th class="px-8 py-6">Carryover to Next Month</th>
                <th class="px-8 py-6">Sessions</th>
                <th class="px-8 py-6">Download</th>
              </tr>
            </thead>
            <tbody class="text-sm font-body divide-y divide-outline-variant/5">
              <tr
                v-for="monthOption in availableMonthOptions"
                :key="monthOption.key"
                class="hover:bg-surface-container-lowest transition-colors"
              >
                <td class="px-8 py-6 font-bold">{{ monthOption.label }}</td>
                <td class="px-8 py-6">{{ getMonthProjectName(monthOption.key) }}</td>
                <td class="px-8 py-6 text-on-surface-variant" data-testid="month-total-hours">
                  {{ getMonthTotalHours(monthOption.key) }}
                </td>
                <td class="px-8 py-6 text-on-surface-variant" data-testid="month-transfer-next">
                  {{ getMonthTransferToNextLabel(monthOption.key) }}
                </td>
                <td class="px-8 py-6">
                  <span
                    class="px-3 py-1 bg-secondary-fixed text-on-secondary-fixed rounded-full text-xs font-bold"
                    >{{ getMonthSessions(monthOption.key) }} sessions</span
                  >
                </td>
                <td class="px-8 py-6">
                  <button
                    class="px-3 py-1 bg-primary text-on-primary rounded-lg text-xs font-bold hover:bg-primary/90"
                    @click="downloadReportForMonth(monthOption.key)"
                  >
                    Download PDF
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p v-if="errorMessage" class="text-error mt-4">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import AppSidebar from '../components/AppSidebar.vue';
import MainTopBar from '../components/MainTopBar.vue';
import { exportProjectReportPdf, getProjectCalendar, getProjects } from '../services/apiClient';
import { useUserSettings } from '../composables/useUserSettings';
import type { Project, Timeslot } from '../types/domain';

/**
 * Reporting screen showing monthly billing and workload exports.
 */
const { userName } = useUserSettings();
const projects = ref<Project[]>([]);
const selectedProjectId = ref('');
const projectStartMonthKey = ref(new Date().toISOString().slice(0, 7));
const allTimeslots = ref<Timeslot[]>([]);
const selectedProjectName = ref('');
const calendarRequestVersion = ref(0);
const errorMessage = ref('');

const selectedProject = computed(
  () => projects.value.find((project) => project.id === selectedProjectId.value) || null
);

const selectedProjectInstitution = computed(() => selectedProject.value?.institution || '—');

const selectedProjectTargetMinutes = computed(() => {
  const targetHours = Number(selectedProject.value?.targetMonthHours || 0);
  return Math.round(targetHours * 60);
});

const selectedProjectTargetLabel = computed(() =>
  selectedProjectTargetMinutes.value > 0
    ? formatDurationMinutes(selectedProjectTargetMinutes.value)
    : '—'
);

const availableMonthOptions = computed(() => {
  const start = monthKeyToDate(projectStartMonthKey.value);
  const end = monthKeyToDate(new Date().toISOString().slice(0, 7));
  const months = [];
  const startTime = start.getTime();
  let cursorTime = new Date(end.getFullYear(), end.getMonth(), 1).getTime();

  while (cursorTime >= startTime) {
    const cursor = new Date(cursorTime);
    const monthKey = dateToMonthKey(cursor);
    months.push({ key: monthKey, label: formatMonthLabel(monthKey) });
    cursor.setMonth(cursor.getMonth() - 1);
    cursorTime = cursor.getTime();
  }

  return months;
});

const newestMonthKey = computed(() => availableMonthOptions.value[0]?.key || '');

function monthKeyToDate(monthKey: string): Date {
  const [year, month] = monthKey.split('-').map(Number);
  return new Date(year, month - 1, 1);
}

function dateToMonthKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

function formatMonthLabel(monthKey: string): string {
  const [year, month] = monthKey.split('-').map(Number);
  const date = new Date(year, month - 1, 1);
  return date.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' });
}

function normalizeMonthKey(dateString: string | undefined): string | null {
  if (!dateString) {
    return null;
  }
  return dateString.slice(0, 7);
}

function compareMonthKeys(left: string, right: string): number {
  return left.localeCompare(right);
}

function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
}

function monthDistanceInclusive(startMonthKey: string, endMonthKey: string): number {
  const start = monthKeyToDate(startMonthKey);
  const end = monthKeyToDate(endMonthKey);
  const months =
    (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth());
  return months + 1;
}

function formatDurationMinutes(totalMinutes: number): string {
  const normalized = Math.max(0, Math.round(totalMinutes || 0));
  const hours = Math.floor(normalized / 60);
  const minutes = normalized % 60;
  return `${hours}h ${String(minutes).padStart(2, '0')}min`;
}

function formatSignedDurationMinutes(totalMinutes: number): string {
  const sign = totalMinutes < 0 ? '-' : '';
  return `${sign}${formatDurationMinutes(Math.abs(totalMinutes))}`;
}

function findFirstTimeslotMonthKey(): string | null {
  const monthKeys = (allTimeslots.value || [])
    .map((slot) => normalizeMonthKey(slot.date))
    .filter((monthKey): monthKey is string => monthKey !== null)
    .sort(compareMonthKeys);
  return monthKeys[0] || null;
}

async function loadProjectCalendarData(projectId: string): Promise<void> {
  if (!projectId) {
    allTimeslots.value = [];
    selectedProjectName.value = '';
    return;
  }

  const requestVersion = ++calendarRequestVersion.value;

  try {
    const calendar = await getProjectCalendar(projectId);
    if (requestVersion !== calendarRequestVersion.value || projectId !== selectedProjectId.value) {
      return;
    }
    allTimeslots.value = calendar.allSlots || [];
    selectedProjectName.value = calendar.projectName || '';
  } catch (error) {
    if (requestVersion !== calendarRequestVersion.value || projectId !== selectedProjectId.value) {
      return;
    }
    errorMessage.value = getErrorMessage(error);
    allTimeslots.value = [];
    selectedProjectName.value = '';
  }
}

async function resolveProjectStartMonth(projectId: string): Promise<string> {
  try {
    const calendar = await getProjectCalendar(projectId);
    const slotMonthKeys = (calendar.allSlots || [])
      .map((slot) => normalizeMonthKey(slot.date))
      .filter((monthKey): monthKey is string => monthKey !== null);

    if (slotMonthKeys.length === 0) {
      return new Date().toISOString().slice(0, 7);
    }

    return slotMonthKeys.sort(compareMonthKeys)[0] ?? new Date().toISOString().slice(0, 7);
  } catch {
    return new Date().toISOString().slice(0, 7);
  }
}

function getMonthReportData(monthKey: string): {
  projectName: string;
  totalMinutes: number;
  sessions: number;
  transferToNextMonthMinutes: number;
} {
  const slotsInMonth = (allTimeslots.value || []).filter((slot) => {
    const slotMonth = normalizeMonthKey(slot.date);
    return slotMonth === monthKey;
  });

  const totalMinutes = slotsInMonth.reduce((sum, slot) => sum + (slot.durationMinutes || 0), 0);
  const firstTimeslotMonthKey = findFirstTimeslotMonthKey();
  const targetMinutes = selectedProjectTargetMinutes.value;

  let transferToNextMonthMinutes = 0;
  if (
    firstTimeslotMonthKey &&
    targetMinutes > 0 &&
    compareMonthKeys(monthKey, firstTimeslotMonthKey) >= 0
  ) {
    const cumulativeMinutes = (allTimeslots.value || [])
      .filter((slot) => {
        const slotMonth = normalizeMonthKey(slot.date);
        return (
          slotMonth &&
          compareMonthKeys(slotMonth, firstTimeslotMonthKey) >= 0 &&
          compareMonthKeys(slotMonth, monthKey) <= 0
        );
      })
      .reduce((sum, slot) => sum + (slot.durationMinutes || 0), 0);

    const targetForCoveredMonths =
      monthDistanceInclusive(firstTimeslotMonthKey, monthKey) * targetMinutes;
    transferToNextMonthMinutes = cumulativeMinutes - targetForCoveredMonths;
  }

  return {
    projectName: selectedProjectName.value,
    totalMinutes,
    sessions: slotsInMonth.length,
    transferToNextMonthMinutes,
  };
}

function getMonthProjectName(monthKey: string): string {
  const data = getMonthReportData(monthKey);
  return data?.projectName || '—';
}

function getMonthTotalHours(monthKey: string): string {
  const data = getMonthReportData(monthKey);
  return formatDurationMinutes(data?.totalMinutes || 0);
}

function getMonthSessions(monthKey: string): number | string {
  const data = getMonthReportData(monthKey);
  return data?.sessions || '0';
}

function getMonthTransferToNextLabel(monthKey: string): string {
  const data = getMonthReportData(monthKey);
  return formatSignedDurationMinutes(data?.transferToNextMonthMinutes || 0);
}

/**
 * Downloads a generated monthly report as PDF.
 */
async function downloadReportForMonth(monthKey: string): Promise<void> {
  errorMessage.value = '';
  try {
  const blob = await exportProjectReportPdf(selectedProjectId.value, monthKey, userName.value);
    const filename = `${selectedProjectId.value}-${monthKey}-report.pdf`;
    const objectUrl = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();

    URL.revokeObjectURL(objectUrl);
  } catch (error) {
    errorMessage.value = getErrorMessage(error);
  }
}

async function generateNewestMonthlyReport(): Promise<void> {
  if (!newestMonthKey.value) {
    return;
  }
  await downloadReportForMonth(newestMonthKey.value);
}

onMounted(async () => {
  try {
    projects.value = await getProjects();
    selectedProjectId.value = projects.value[0]?.id || '';
    projectStartMonthKey.value = await resolveProjectStartMonth(selectedProjectId.value);
    await loadProjectCalendarData(selectedProjectId.value);
  } catch (error) {
    console.warn('Using fallback report/project data because API is unavailable.', error);
    errorMessage.value = getErrorMessage(error);
    projects.value = [
      {
        id: 'math-grade-10',
        name: 'Math Grade 10',
        institution: 'University Teaching Lab',
        targetMonthHours: 12.5,
      },
    ];
    selectedProjectId.value = 'math-grade-10';
  }
});

watch(selectedProjectId, async () => {
  projectStartMonthKey.value = await resolveProjectStartMonth(selectedProjectId.value);
  await loadProjectCalendarData(selectedProjectId.value);
});
</script>
