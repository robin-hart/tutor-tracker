<template>
  <div class="bg-surface font-body text-on-surface min-h-screen">
    <AppSidebar />
    <MainTopBar
      v-model="searchText"
      search-placeholder="Search projects..."
      :search-disabled="apiUnavailable"
    >
      <template #tabs>
        <nav class="flex gap-6">
          <a class="text-primary font-bold border-b-2 border-primary pb-1 font-manrope text-sm"
            >All Projects</a
          >
          <a class="text-on-surface-variant font-manrope text-sm">Active Sessions</a>
        </nav>
      </template>
    </MainTopBar>

    <main class="ml-72 pt-28 px-12 pb-12 min-h-screen">
      <section class="mb-12 flex justify-between items-end">
        <div>
          <h1 class="text-5xl font-extrabold font-headline tracking-tight mb-2">Projects</h1>
          <p class="text-on-surface-variant text-lg max-w-xl">
            Manage your teaching portfolio and track engagement across all active educational
            modules.
          </p>
        </div>
        <button
          class="premium-gradient flex items-center gap-2 px-8 py-4 rounded-xl text-on-primary font-headline font-bold shadow-xl"
          @click="showCreateProject = !showCreateProject"
        >
          <span class="material-symbols-outlined">add</span>
          New Project
        </button>
      </section>

      <section
        v-if="showCreateProject"
        class="mb-8 bg-surface-container-lowest rounded-xl p-6 grid grid-cols-1 md:grid-cols-6 gap-4"
      >
        <div class="md:col-span-6 flex items-center justify-between">
          <h2 class="text-lg font-bold font-headline">Create New Project</h2>
          <button
            type="button"
            class="h-9 w-9 rounded-lg bg-surface-container-low text-on-surface hover:bg-surface-container-high flex items-center justify-center"
            aria-label="Close create project form"
            @click="closeCreateProject"
          >
            <span class="material-symbols-outlined text-base">close</span>
          </button>
        </div>

        <div class="md:col-span-2">
          <label class="text-xs text-on-surface-variant uppercase tracking-wider block mb-2"
            >Project Name</label
          >
          <input
            v-model.trim="newProject.name"
            class="w-full bg-surface-container-low rounded-lg px-3 py-2"
            placeholder="Project name"
            type="text"
          />
        </div>

        <div class="md:col-span-3">
          <label class="text-xs text-on-surface-variant uppercase tracking-wider block mb-2"
            >Category</label
          >
          <div class="flex flex-wrap gap-2">
            <button
              v-for="category in categoryOptions"
              :key="category"
              type="button"
              class="px-3 py-1.5 rounded-full text-xs font-bold border transition"
              :class="
                newProject.category === category && !showNewCategoryInput
                  ? 'bg-primary text-white border-primary'
                  : 'bg-surface-container-low text-on-surface border-outline-variant hover:border-primary/50'
              "
              @click="selectCategory(category)"
            >
              {{ category }}
            </button>
            <button
              type="button"
              class="px-3 py-1.5 rounded-full text-xs font-bold border transition"
              :class="
                showNewCategoryInput
                  ? 'bg-primary text-white border-primary'
                  : 'bg-surface-container-low text-on-surface border-outline-variant hover:border-primary/50'
              "
              @click="openNewCategoryInput"
            >
              + New category
            </button>
          </div>
          <input
            v-if="showNewCategoryInput"
            v-model.trim="newCategory"
            class="mt-3 w-full bg-surface-container-low rounded-lg px-3 py-2"
            placeholder="Enter new category"
            type="text"
          />
        </div>

        <div class="md:col-span-6 flex justify-end gap-3">
          <button
            type="button"
            class="px-4 py-2 rounded-lg bg-surface-container-low text-on-surface font-bold"
            @click="closeCreateProject"
          >
            Cancel
          </button>
          <button
            class="px-4 py-2 rounded-lg bg-primary text-white font-bold"
            @click="createNewProject"
          >
            Create
          </button>
        </div>
      </section>

      <div class="grid grid-cols-12 gap-6 mb-12">
        <div class="col-span-12 md:col-span-4 bg-surface-container-lowest p-8 rounded-xl shadow-sm">
          <span class="text-on-surface-variant text-sm uppercase tracking-widest mb-4 block"
            >Total Focus Hours</span
          >
          <div class="flex items-baseline gap-2">
            <span class="text-5xl font-black font-headline text-primary">{{
              totalHours.hours
            }}</span>
            <span class="text-on-surface-variant font-medium">hrs</span>
            <span class="text-3xl font-black font-headline text-primary">{{
              totalHours.minutes
            }}</span>
            <span class="text-on-surface-variant font-medium">min</span>
          </div>
        </div>
        <div class="col-span-12 md:col-span-4 bg-surface-container-low p-8 rounded-xl">
          <span class="text-on-surface-variant text-sm uppercase tracking-widest mb-4 block"
            >Active Projects</span
          >
          <span class="text-5xl font-black font-headline">{{ projects.length }}</span>
        </div>
        <div class="col-span-12 md:col-span-4 bg-secondary-fixed p-8 rounded-xl">
          <span class="text-on-secondary-fixed-variant text-sm uppercase tracking-widest mb-4 block"
            >Current Session</span
          >
          <span class="block text-xl font-bold font-headline text-on-secondary-fixed"
            >Math Grade 10</span
          >
          <span class="text-sm text-on-secondary-fixed-variant">00:42:15 elapsed</span>
        </div>
      </div>

      <section class="mb-6 flex items-center justify-end gap-4">
        <span class="text-sm text-on-surface-variant">{{ filteredProjects.length }} shown</span>
      </section>

      <SearchActiveIndicator
        v-if="hasActiveFilter && !apiUnavailable"
        class="mb-6"
        :query="searchText"
        :shown-count="filteredProjects.length"
        :total-count="projects.length"
        entity-label="projects"
        @clear="searchText = ''"
      />

      <section v-if="isLoading" class="mb-8">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 animate-pulse">
          <div v-for="item in 3" :key="item" class="bg-surface-container-lowest rounded-xl p-8">
            <div class="h-4 w-24 bg-surface-container-high rounded mb-6"></div>
            <div class="h-8 w-3/4 bg-surface-container-high rounded mb-8"></div>
            <div class="h-4 w-full bg-surface-container-high rounded mb-3"></div>
            <div class="h-4 w-2/3 bg-surface-container-high rounded mb-6"></div>
            <div class="h-2 w-full bg-surface-container-high rounded"></div>
          </div>
        </div>
      </section>
      <p v-if="errorMessage && !apiUnavailable" class="text-error mb-4">{{ errorMessage }}</p>

      <section
        v-if="apiUnavailable"
        class="mb-8 bg-surface-container-lowest border border-outline-variant rounded-xl p-6"
      >
        <div class="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <h2 class="text-xl font-bold font-headline mb-2">Service currently unavailable</h2>
            <p class="text-on-surface-variant max-w-2xl">
              We are unable to reach the server right now. Please check your connection and try
              again.
            </p>
          </div>
          <button
            class="bg-primary text-white rounded-lg px-4 py-2 font-bold"
            :disabled="isLoading"
            @click="loadProjects"
          >
            Retry
          </button>
        </div>
      </section>

      <div v-if="!apiUnavailable" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <article
          v-for="project in filteredProjects"
          :key="project.id"
          class="group bg-surface-container-lowest rounded-xl p-8 hover:shadow-2xl transition-all cursor-pointer"
          @click="openCalendar(project.id)"
        >
          <div class="flex justify-between items-start mb-6">
            <span
              class="px-3 py-1 bg-secondary-fixed text-on-secondary-fixed-variant text-xs font-bold rounded-full"
              >{{ project.category }}</span
            >
          </div>
          <h3 class="text-2xl font-bold font-headline mb-8">{{ project.name }}</h3>
          <div class="flex justify-between items-end">
            <div>
              <span class="text-xs text-on-surface-variant uppercase tracking-wider block mb-1"
                >Total Hours</span
              >
              <div class="flex items-baseline gap-1.5">
                <span class="text-2xl font-black font-headline">{{
                  formatProjectHours(project.totalHours).hours
                }}</span>
                <span class="text-xs text-on-surface-variant font-medium">hrs</span>
                <span class="text-lg font-black font-headline">{{
                  formatProjectHours(project.totalHours).minutes
                }}</span>
                <span class="text-xs text-on-surface-variant font-medium">min</span>
              </div>
            </div>
            <div class="text-right">
              <span class="text-xs text-on-surface-variant uppercase tracking-wider block mb-1"
                >This Month</span
              >
              <div class="flex items-baseline gap-1.5 justify-end">
                <span class="text-xl font-bold font-headline text-primary">{{
                  formatProjectHours(project.monthHours).hours
                }}</span>
                <span class="text-xs text-on-surface-variant font-medium">hrs</span>
                <span class="text-sm font-black font-headline text-primary">{{
                  formatProjectHours(project.monthHours).minutes
                }}</span>
                <span class="text-xs text-on-surface-variant font-medium">min</span>
              </div>
            </div>
          </div>
          <div class="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden mt-6">
            <div
              class="h-full bg-primary rounded-full"
              :style="{ width: `${project.completionPercent}%` }"
            ></div>
          </div>
          <div class="mt-6 flex gap-3">
            <RouterLink
              :to="{ name: 'student-management', params: { projectId: project.id } }"
              class="text-sm font-bold text-primary"
              @click.stop
              >Students</RouterLink
            >
            <button
              class="text-sm font-bold text-error ml-auto"
              @click.stop="openDeleteConfirm(project.id, project.name)"
            >
              Delete
            </button>
          </div>
        </article>
      </div>

      <p
        v-if="!isLoading && !apiUnavailable && hasActiveFilter && filteredProjects.length === 0"
        class="text-on-surface-variant mt-8"
      >
        No projects match your filter.
      </p>
      <p
        v-if="!isLoading && !apiUnavailable && !hasActiveFilter && projects.length === 0"
        class="text-on-surface-variant mt-8"
      >
        No projects available yet.
      </p>

      <ConfirmDialog
        :is-open="showDeleteConfirm"
        title="Delete Project?"
        @cancel="cancelDelete"
        @confirm="confirmDelete"
      >
        Are you sure you want to delete <strong>{{ projectToDelete?.name }}</strong
        >? This will permanently remove all associated students, timeslots, and reports.
      </ConfirmDialog>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppSidebar from '../components/AppSidebar.vue';
import ConfirmDialog from '../components/ConfirmDialog.vue';
import MainTopBar from '../components/MainTopBar.vue';
import SearchActiveIndicator from '../components/SearchActiveIndicator.vue';
import { createProject, getProjects, deleteProject } from '../services/apiClient';
import { filterProjects } from '../utils/projectFilter';
import { formatHoursToHM } from '../utils/timeFormatter';

/**
 * Dashboard view for listing tutoring projects and high-level metrics.
 */
const projects = ref([]);
const isLoading = ref(false);
const errorMessage = ref('');
const searchText = ref('');
const showCreateProject = ref(false);
const router = useRouter();
const newProject = ref({
  name: '',
  category: 'GENERAL',
  totalHours: 0,
  monthHours: 0,
  completionPercent: 0,
});

const showDeleteConfirm = ref(false);
const projectToDelete = ref(null);
const apiUnavailable = ref(false);
const showNewCategoryInput = ref(false);
const newCategory = ref('');

const categoryOptions = computed(() => {
  const categories = new Set(['GENERAL']);
  projects.value.forEach((project) => {
    const category = String(project?.category || '').trim();
    if (category) {
      categories.add(category);
    }
  });
  return Array.from(categories).sort((a, b) => a.localeCompare(b));
});

const totalHours = computed(() => {
  const sum = projects.value.reduce((sum, item) => sum + Number(item.totalHours), 0);
  return formatHoursToHM(sum);
});
const filteredProjects = computed(() => {
  return filterProjects(projects.value, searchText.value);
});
const hasActiveFilter = computed(() => searchText.value.trim().length > 0);

/**
 * Opens the calendar route for a selected project.
 *
 * @param {string} projectId selected project id
 */
function openCalendar(projectId) {
  router.push({ name: 'project-calendar', params: { projectId } });
}

function closeCreateProject() {
  showCreateProject.value = false;
  showNewCategoryInput.value = false;
  newCategory.value = '';
  newProject.value = {
    name: '',
    category: 'GENERAL',
    totalHours: 0,
    monthHours: 0,
    completionPercent: 0,
  };
}

async function createNewProject() {
  const resolvedCategory = showNewCategoryInput.value
    ? newCategory.value.trim()
    : String(newProject.value.category || '').trim();

  if (!newProject.value.name || !resolvedCategory) {
    return;
  }

  try {
    const payload = {
      ...newProject.value,
      category: resolvedCategory,
      totalHours: 0,
      monthHours: 0,
      completionPercent: 0,
    };
    const created = await createProject(payload);
    projects.value = [created, ...projects.value];
    closeCreateProject();
  } catch (error) {
    errorMessage.value = error.message;
  }
}

function selectCategory(category) {
  newProject.value.category = category;
  showNewCategoryInput.value = false;
  newCategory.value = '';
}

function openNewCategoryInput() {
  showNewCategoryInput.value = true;
}

/**
 * Opens the delete confirmation dialog for a project.
 *
 * @param {string} projectId project id to delete
 * @param {string} projectName project name for display
 */
function openDeleteConfirm(projectId, projectName) {
  projectToDelete.value = { id: projectId, name: projectName };
  showDeleteConfirm.value = true;
}

/**
 * Confirms and executes project deletion.
 */
async function confirmDelete() {
  if (!projectToDelete.value) {
    return;
  }
  try {
    await deleteProject(projectToDelete.value.id);
    projects.value = projects.value.filter((p) => p.id !== projectToDelete.value.id);
    showDeleteConfirm.value = false;
    projectToDelete.value = null;
  } catch (error) {
    errorMessage.value = error.message;
    showDeleteConfirm.value = false;
    projectToDelete.value = null;
  }
}

/**
 * Cancels the delete confirmation dialog.
 */
function cancelDelete() {
  showDeleteConfirm.value = false;
  projectToDelete.value = null;
}

/**
 * Formats hours as "Xh45" format
 * @param {number} hours decimal hours
 * @returns {string} formatted time
 */
function formatProjectHours(hours) {
  return formatHoursToHM(hours);
}

async function loadProjects() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    projects.value = await getProjects();
    apiUnavailable.value = false;
  } catch (error) {
    console.warn('Projects API is unavailable.', error);
    apiUnavailable.value = true;
    projects.value = [];
  } finally {
    isLoading.value = false;
  }
}

onMounted(() => {
  loadProjects();
});
</script>
