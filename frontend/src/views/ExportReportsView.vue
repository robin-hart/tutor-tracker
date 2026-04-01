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

      <section class="mb-8 grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
        <label class="block">
          <span class="text-xs text-on-surface-variant uppercase tracking-widest">Project</span>
          <select
            v-model="selectedProjectId"
            class="mt-2 w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20"
          >
            <option v-for="project in projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </option>
          </select>
        </label>
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
                <th class="px-8 py-6">Download</th>
              </tr>
            </thead>
            <tbody class="text-sm font-body divide-y divide-outline-variant/5">
              <tr
                v-for="monthOption in availableMonthOptions"
                :key="monthOption.key"
                class="hover:bg-surface-container-lowest transition-colors"
              >
                <td class="px-8 py-6">
                  <span class="font-bold">{{ monthOption.label }}</span>
                  <span class="text-on-surface-variant"> ({{ monthOption.key }})</span>
                </td>
                <td class="px-8 py-6">
                  <button
                    class="px-3 py-1 bg-secondary-fixed text-on-secondary-fixed rounded-lg text-xs font-bold"
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

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import AppSidebar from '../components/AppSidebar.vue';
import MainTopBar from '../components/MainTopBar.vue';
import { exportProjectReportPdf, getProjectCalendar, getProjects } from '../services/apiClient';

/**
 * Reporting screen showing monthly billing and workload exports.
 */
const projects = ref([]);
const selectedProjectId = ref('');
const projectStartMonthKey = ref(new Date().toISOString().slice(0, 7));
const errorMessage = ref('');

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

function monthKeyToDate(monthKey) {
  const [year, month] = monthKey.split('-').map(Number);
  return new Date(year, month - 1, 1);
}

function dateToMonthKey(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

function formatMonthLabel(monthKey) {
  const [year, month] = monthKey.split('-').map(Number);
  const date = new Date(year, month - 1, 1);
  return date.toLocaleDateString('de-DE', { month: 'long', year: 'numeric' });
}

function normalizeMonthKey(dateString) {
  if (!dateString) {
    return null;
  }
  return dateString.slice(0, 7);
}

async function resolveProjectStartMonth(projectId) {
  const selectedProject = projects.value.find((project) => project.id === projectId);
  const createdMonthKey = normalizeMonthKey(selectedProject?.createdAt);
  if (createdMonthKey) {
    return createdMonthKey;
  }

  try {
    const calendar = await getProjectCalendar(projectId);
    const slotMonthKeys = (calendar.allSlots || [])
      .map((slot) => normalizeMonthKey(slot.date))
      .filter(Boolean);
    const candidates = [...slotMonthKeys];

    if (candidates.length === 0) {
      return new Date().toISOString().slice(0, 7);
    }

    return candidates.sort()[0];
  } catch {
    return new Date().toISOString().slice(0, 7);
  }
}

async function downloadReportForMonth(monthKey) {
  errorMessage.value = '';
  try {
    const blob = await exportProjectReportPdf(selectedProjectId.value, monthKey);
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
    errorMessage.value = error.message;
  }
}

async function generateNewestMonthlyReport() {
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
  } catch (error) {
    console.warn('Using fallback report/project data because API is unavailable.', error);
    errorMessage.value = error.message;
    projects.value = [{ id: 'math-grade-10', name: 'Math Grade 10' }];
    selectedProjectId.value = 'math-grade-10';
  }
});

watch(selectedProjectId, async () => {
  projectStartMonthKey.value = await resolveProjectStartMonth(selectedProjectId.value);
});
</script>
