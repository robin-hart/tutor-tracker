<template>
  <div class="bg-surface font-body text-on-surface antialiased min-h-screen">
    <AppSidebar />

    <main class="ml-72 p-16">
      <h2 class="font-headline text-5xl font-extrabold tracking-tight mb-8">Session Log</h2>
    </main>

    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-on-background/40 backdrop-blur-md"
    >
      <div
        class="w-full max-w-lg mx-4 glass-panel rounded-2xl shadow-2xl overflow-hidden border border-white/20"
      >
        <div class="px-8 pt-8 pb-4 flex justify-between items-center">
          <div>
            <h3 class="font-headline text-2xl font-extrabold tracking-tight">
              {{ isEditMode ? 'Edit Timeslot' : 'New Timeslot' }}
            </h3>
            <p class="text-on-surface-variant text-sm font-medium">Define your session details</p>
          </div>
          <RouterLink
            :to="closeLink"
            class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container-highest"
          >
            <span class="material-symbols-outlined text-on-surface-variant">close</span>
          </RouterLink>
        </div>

        <form class="px-8 py-6 space-y-6" @submit.prevent="onSave">
          <div class="space-y-2">
            <label
              for="timeslot-title"
              class="block text-xs font-bold uppercase tracking-wider text-on-surface-variant ml-1"
              >Title</label
            >
            <input
              id="timeslot-title"
              v-model="form.title"
              class="w-full px-5 py-4 bg-surface-container-low border-none rounded-xl focus:ring-2 focus:ring-primary/20"
              placeholder="e.g. Integration Workshop"
              type="text"
              required
            />
          </div>
          <div class="space-y-2">
            <label
              for="timeslot-description"
              class="block text-xs font-bold uppercase tracking-wider text-on-surface-variant ml-1"
              >Description</label
            >
            <textarea
              id="timeslot-description"
              v-model="form.description"
              class="w-full px-5 py-4 bg-surface-container-low border-none rounded-xl focus:ring-2 focus:ring-primary/20 resize-none"
              rows="4"
              placeholder="Briefly describe the work performed..."
            ></textarea>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <input
              v-model.number="form.durationMinutes"
              class="px-4 py-3 bg-surface-container-low rounded-xl"
              type="number"
              min="15"
              step="15"
              placeholder="Duration"
              required
            />
            <input
              v-model="form.date"
              class="px-4 py-3 bg-surface-container-low rounded-xl"
              type="date"
              required
            />
          </div>
          <div>
            <input
              v-model="form.startTime"
              class="px-4 py-3 bg-surface-container-low rounded-xl w-full"
              type="time"
              required
            />
          </div>
          <p v-if="errorMessage" class="text-sm text-error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="text-sm text-primary">{{ successMessage }}</p>
          <div class="px-0 pt-4 flex items-center justify-end gap-3">
            <RouterLink :to="closeLink" class="px-6 py-3 text-sm font-bold text-on-surface-variant"
              >Cancel</RouterLink
            >
            <button
              :disabled="isSaving || isLoading"
              class="px-10 py-3 premium-gradient text-white rounded-lg font-bold text-sm shadow-xl disabled:opacity-60"
              type="submit"
            >
              {{ isSaving ? 'Saving...' : isEditMode ? 'Update Session' : 'Save Session' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppSidebar from '../components/AppSidebar.vue';
import { getProjectTimeslot, saveTimeslot, updateProjectTimeslot } from '../services/apiClient';

/**
 * Modal-like page for editing and saving a single tutoring timeslot.
 */
const router = useRouter();
const route = useRoute();
const isSaving = ref(false);
const isLoading = ref(false);
const errorMessage = ref('');
const successMessage = ref('');
const targetProjectId = computed(() => String(route.query.projectId || 'math-grade-10'));
const targetMonth = computed(() => String(route.query.month || ''));
const timeslotId = computed(() => String(route.query.timeslotId || ''));
const isEditMode = computed(() => timeslotId.value.length > 0);
const closeLink = computed(() => ({
  name: 'project-calendar',
  params: { projectId: targetProjectId.value },
  query: targetMonth.value ? { month: targetMonth.value } : undefined,
}));

const form = reactive({
  title: '',
  description: '',
  durationMinutes: 90,
  date: String(route.query.date || new Date().toISOString().slice(0, 10)),
  startTime: String(route.query.startTime || '14:00').slice(0, 5),
});

async function loadExistingTimeslot() {
  if (!isEditMode.value) {
    return;
  }

  isLoading.value = true;
  errorMessage.value = '';
  try {
    const slot = await getProjectTimeslot(targetProjectId.value, timeslotId.value);
    form.title = slot.title || '';
    form.description = slot.description || '';
    form.durationMinutes = Number(slot.durationMinutes) || 60;
    form.date = slot.date || form.date;
    form.startTime = String(slot.startTime || form.startTime).slice(0, 5);
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    isLoading.value = false;
  }
}

async function onSave() {
  isSaving.value = true;
  errorMessage.value = '';
  successMessage.value = '';
  try {
    if (isEditMode.value) {
      await updateProjectTimeslot(targetProjectId.value, timeslotId.value, form);
      successMessage.value = 'Timeslot updated successfully.';
    } else {
      await saveTimeslot(targetProjectId.value, form);
      successMessage.value = 'Timeslot saved successfully.';
    }
    setTimeout(() => {
      router.push(closeLink.value);
    }, 300);
  } catch (error) {
    errorMessage.value = error.message;
    console.warn('Could not persist timeslot to API.', error);
  } finally {
    isSaving.value = false;
  }
}

watch(
  [targetProjectId, timeslotId],
  () => {
    loadExistingTimeslot();
  },
  { immediate: true }
);
</script>
