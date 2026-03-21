<template>
  <div class="text-on-surface bg-background min-h-screen">
    <AppSidebar />
    <MainTopBar search-placeholder="Search reports or sessions...">
      <template #tabs>
        <nav class="flex items-center gap-6">
          <a class="text-primary font-bold border-b-2 border-primary pb-1 font-manrope text-sm">All Projects</a>
          <a class="text-on-surface-variant font-manrope text-sm">Active Sessions</a>
        </nav>
      </template>
    </MainTopBar>

    <main class="ml-72 pt-32 px-12 pb-20">
      <div class="mb-12">
        <h1 class="font-headline text-5xl font-extrabold tracking-tight mb-2">Reports & Exports</h1>
        <p class="text-on-surface-variant text-lg max-w-2xl">Analyze performance trends and generate professional documentation for your billing and session history.</p>
      </div>

      <section class="mb-8 grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
        <label class="block">
          <span class="text-xs text-on-surface-variant uppercase tracking-widest">Project</span>
          <select v-model="selectedProjectId" class="mt-2 w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20">
            <option v-for="project in projects" :key="project.id" :value="project.id">{{ project.name }}</option>
          </select>
        </label>
        <label class="block">
          <span class="text-xs text-on-surface-variant uppercase tracking-widest">Filter by project</span>
          <input v-model.trim="projectFilter" class="mt-2 w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20" placeholder="Project name" type="text" />
        </label>
        <label class="block">
          <span class="text-xs text-on-surface-variant uppercase tracking-widest">Month Key</span>
          <input v-model.trim="monthFilter" class="mt-2 w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20" placeholder="2026-03" type="text" />
        </label>
        <div class="bg-surface-container-lowest rounded-xl px-4 py-3">
          <p class="text-xs text-on-surface-variant uppercase tracking-widest mb-1">Filtered Gross Amount</p>
          <p class="font-headline text-2xl font-bold">${{ filteredGross.toFixed(2) }}</p>
        </div>
      </section>

      <section class="mb-8 flex items-center gap-3">
        <button
          class="premium-gradient text-white px-4 py-2 rounded-lg font-bold text-sm"
          :disabled="!selectedProjectId || !monthFilter"
          @click="generateMonthlyReport"
        >
          Generate Project Monthly Report
        </button>
      </section>

      <div class="bg-surface-container-low rounded-2xl overflow-hidden shadow-[0_20px_40px_rgba(11,28,48,0.04)]">
        <div class="p-8 flex items-center justify-between bg-surface-container-low border-b border-outline-variant/10">
          <h2 class="font-headline font-bold text-2xl">Monthly Breakdowns</h2>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="text-on-surface-variant uppercase text-[10px] font-bold tracking-[0.2em]">
                <th class="px-8 py-6">Month / Period</th>
                <th class="px-8 py-6">Project Name</th>
                <th class="px-8 py-6">Total Hours</th>
                <th class="px-8 py-6">Sessions</th>
                <th class="px-8 py-6">Gross Amount</th>
              </tr>
            </thead>
            <tbody class="text-sm font-body divide-y divide-outline-variant/5">
              <tr v-for="row in filteredReports" :key="`${row.month}-${row.projectName}`" class="hover:bg-surface-container-lowest transition-colors">
                <td class="px-8 py-6 font-bold">{{ row.month }}</td>
                <td class="px-8 py-6">{{ row.projectName }}</td>
                <td class="px-8 py-6 text-on-surface-variant">{{ row.totalHours }} hrs</td>
                <td class="px-8 py-6"><span class="px-3 py-1 bg-secondary-fixed text-on-secondary-fixed rounded-full text-xs font-bold">{{ row.sessions }} sessions</span></td>
                <td class="px-8 py-6 font-bold">${{ row.grossAmount.toFixed(2) }}</td>
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
import { generateProjectReport, getProjectReports, getProjects } from '../services/apiClient';

/**
 * Reporting screen showing monthly billing and workload exports.
 */
const reports = ref([]);
const projects = ref([]);
const selectedProjectId = ref('');
const projectFilter = ref('');
const monthFilter = ref(new Date().toISOString().slice(0, 7));
const errorMessage = ref('');

const filteredReports = computed(() => {
  return reports.value.filter((row) => {
    const byProject = projectFilter.value ? row.projectName.toLowerCase().includes(projectFilter.value.toLowerCase()) : true;
    const byMonth = monthFilter.value ? row.month.toLowerCase().includes(monthFilter.value.toLowerCase()) || row.month.includes(monthFilter.value) : true;
    return byProject && byMonth;
  });
});

const filteredGross = computed(() => filteredReports.value.reduce((sum, row) => sum + Number(row.grossAmount), 0));

async function loadProjectReports() {
  if (!selectedProjectId.value) {
    reports.value = [];
    return;
  }
  try {
    reports.value = await getProjectReports(selectedProjectId.value);
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function generateMonthlyReport() {
  errorMessage.value = '';
  try {
    await generateProjectReport(selectedProjectId.value, monthFilter.value);
    await loadProjectReports();
  } catch (error) {
    errorMessage.value = error.message;
  }
}

onMounted(async () => {
  try {
    projects.value = await getProjects();
    selectedProjectId.value = projects.value[0]?.id || '';
    await loadProjectReports();
  } catch (error) {
    console.warn('Using fallback report/project data because API is unavailable.', error);
    errorMessage.value = error.message;
    projects.value = [{ id: 'math-grade-10', name: 'Math Grade 10' }];
    selectedProjectId.value = 'math-grade-10';
    reports.value = [
      { month: 'March 2026', projectName: 'Math Grade 10', totalHours: 6.5, sessions: 3, grossAmount: 390 }
    ];
  }
});

watch(selectedProjectId, async () => {
  await loadProjectReports();
});
</script>
