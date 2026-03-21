<template>
  <div class="bg-surface text-on-surface font-body min-h-screen">
    <AppSidebar />
    <MainTopBar v-model="searchText" search-placeholder="Search slots...">
      <template #tabs>
        <nav class="flex gap-8">
          <a class="text-primary font-bold border-b-2 border-primary pb-1 font-manrope text-sm">Calendar</a>
          <RouterLink :to="{ name: 'student-management', params: { projectId } }" class="text-on-surface-variant font-manrope text-sm">Students</RouterLink>
        </nav>
      </template>
    </MainTopBar>

    <main class="ml-72 pt-32 px-12 pb-20">
      <section class="flex flex-col lg:flex-row justify-between items-start lg:items-end gap-8 mb-10">
        <div class="space-y-2">
          <span class="text-primary font-bold tracking-[0.2em] uppercase text-xs">Active Tutoring Project</span>
          <h2 class="text-6xl font-extrabold tracking-tighter leading-none">{{ projectName }}</h2>
        </div>
      </section>

      <div class="grid grid-cols-12 gap-8 mb-10">
        <div class="col-span-12 md:col-span-6 bg-surface-container-lowest p-8 rounded-xl shadow-sm">
          <span class="text-on-surface-variant text-sm uppercase tracking-widest mb-4 block">Total Hours</span>
          <div class="flex items-baseline gap-2">
            <span class="text-5xl font-black font-headline text-primary">{{ formattedTotalHours.hours }}</span>
            <span class="text-on-surface-variant font-medium">hrs</span>
            <span class="text-3xl font-black font-headline text-primary">{{ formattedTotalHours.minutes }}</span>
            <span class="text-on-surface-variant font-medium">min</span>
          </div>
        </div>
        <div class="col-span-12 md:col-span-6 bg-surface-container-lowest p-8 rounded-xl shadow-sm">
          <span class="text-on-surface-variant text-sm uppercase tracking-widest mb-4 block">This Month</span>
          <div class="flex items-baseline gap-2">
            <span class="text-5xl font-black font-headline text-primary">{{ formattedMonthHours.hours }}</span>
            <span class="text-on-surface-variant font-medium">hrs</span>
            <span class="text-3xl font-black font-headline text-primary">{{ formattedMonthHours.minutes }}</span>
            <span class="text-on-surface-variant font-medium">min</span>
          </div>
        </div>
      </div>

      <section v-if="hasActiveSearch" class="bg-surface-container-lowest rounded-2xl p-8 shadow-sm">
        <div class="flex items-center justify-between mb-6 gap-4">
          <h3 class="text-2xl font-black font-manrope">Search Results</h3>
          <span class="text-sm text-on-surface-variant">{{ filteredSlotResults.length }} slot{{ filteredSlotResults.length === 1 ? '' : 's' }} found</span>
        </div>

        <div class="space-y-4">
          <article v-for="slot in filteredSlotResults" :key="slot.id" class="bg-white p-5 rounded-xl shadow-sm border-l-4 border-primary">
            <div class="flex justify-between items-start mb-2 gap-3">
              <div>
                <p class="text-[11px] text-on-surface-variant font-bold mb-1">{{ slot.date }}</p>
                <h4 class="font-bold text-sm">{{ slot.title }}</h4>
              </div>
              <span class="text-[10px] bg-primary/10 text-primary px-2.5 py-1 rounded-full font-black">{{ formatTime(slot.startTime) }}</span>
            </div>
            <p class="text-[11px] text-on-surface-variant font-bold mb-2">{{ formatPeriod(slot.startTime, slot.durationMinutes) }}</p>
            <p class="text-xs text-on-surface-variant">{{ slot.description || 'No details provided.' }}</p>
            <div class="mt-3 flex items-center gap-2">
              <RouterLink
                :to="{
                  name: 'timeslot-editor',
                  query: {
                    projectId,
                    timeslotId: slot.id,
                    date: slot.date,
                    startTime: slot.startTime,
                    month: monthKey
                  }
                }"
                class="text-[11px] font-black px-2.5 py-1.5 rounded-md bg-surface-container-low"
              >
                Edit
              </RouterLink>
              <button
                type="button"
                class="text-[11px] font-black px-2.5 py-1.5 rounded-md bg-error/10 text-error"
                @click="openDeleteConfirm(slot)"
              >
                Delete
              </button>
            </div>
          </article>
        </div>

        <p v-if="!isLoading && filteredSlotResults.length === 0" class="text-sm text-on-surface-variant mt-6">
          No slots found for this search.
        </p>
      </section>

      <div v-else class="grid grid-cols-12 gap-8">
        <div class="col-span-12 lg:col-span-8 bg-surface-container-lowest rounded-2xl p-8 shadow-sm">
          <div class="flex items-center justify-between mb-8 gap-4">
            <h3 class="text-2xl font-black font-manrope">{{ monthLabel }}</h3>
            <div class="flex items-center gap-2">
              <button @click="goToPreviousMonth" class="px-3 py-2 bg-surface-container-low rounded-lg text-sm font-bold">Prev</button>
              <button @click="goToNextMonth" class="px-3 py-2 bg-surface-container-low rounded-lg text-sm font-bold">Next</button>
            </div>
          </div>
          <div class="grid grid-cols-7 gap-px bg-outline-variant/20 rounded-xl">
            <div v-for="day in weekdays" :key="day" class="bg-surface-container-low/30 text-center text-[10px] font-black uppercase tracking-widest py-3">{{ day }}</div>
            <button
              v-for="cell in calendarCells"
              :key="cell.key"
              @click="selectDay(cell.dateIso, cell.inMonth)"
              class="aspect-square p-3 text-left transition-all duration-200"
              :class="[
                selectedDate === cell.dateIso
                  ? 'relative z-10 bg-primary text-white shadow-[0_10px_18px_rgba(5,106,192,0.30)] border-2 border-primary/95'
                  : (cell.inMonth ? 'bg-white hover:bg-surface-container-low/40' : 'bg-surface-container-low/30 opacity-60')
              ]"
              type="button"
            >
              <span
                class="font-bold"
                :class="selectedDate === cell.dateIso ? 'text-white' : (cell.inMonth ? 'text-on-surface' : 'text-outline/40')"
              >
                {{ cell.day }}
              </span>
              <div
                v-if="slotCountByDate[cell.dateIso]"
                class="mt-2 text-[9px] p-1.5 rounded-md leading-tight font-bold"
                :class="selectedDate === cell.dateIso ? 'bg-white text-primary border border-primary/20' : 'bg-primary text-white'"
              >
                {{ slotCountByDate[cell.dateIso] }} slot{{ slotCountByDate[cell.dateIso] > 1 ? 's' : '' }}
              </div>
            </button>
          </div>
          <p v-if="isLoading" class="text-sm text-on-surface-variant mt-4">Loading project slots...</p>
          <p v-if="errorMessage" class="text-sm text-error mt-4">{{ errorMessage }}</p>
        </div>

        <div class="col-span-12 lg:col-span-4 bg-surface-container-low rounded-2xl p-8">
          <div class="flex items-center justify-between mb-8">
            <h3 class="text-xl font-black font-manrope">{{ selectedDayTitle }}</h3>
            <RouterLink
              :to="{
                name: 'timeslot-editor',
                query: {
                  projectId,
                  date: selectedDate,
                  month: monthKey
                }
              }"
              class="bg-white p-2 rounded-lg text-primary shadow-sm"
            >
              <span class="material-symbols-outlined">add</span>
            </RouterLink>
          </div>
          <div class="space-y-4">
            <div v-for="slot in selectedDaySlots" :key="slot.id" class="bg-white p-5 rounded-xl shadow-sm border-l-4 border-primary">
              <div class="flex justify-between items-start mb-2">
                <h4 class="font-bold text-sm">{{ slot.title }}</h4>
                <span class="text-[10px] bg-primary/10 text-primary px-2.5 py-1 rounded-full font-black">{{ formatTime(slot.startTime) }}</span>
              </div>
              <p class="text-[11px] text-on-surface-variant font-bold mb-2">{{ formatPeriod(slot.startTime, slot.durationMinutes) }}</p>
              <p class="text-xs text-on-surface-variant">{{ slot.description || 'No details provided.' }}</p>
              <div class="mt-3 flex items-center gap-2">
                <RouterLink
                  :to="{
                    name: 'timeslot-editor',
                    query: {
                      projectId,
                      timeslotId: slot.id,
                      date: slot.date,
                      startTime: slot.startTime,
                      month: monthKey
                    }
                  }"
                  class="text-[11px] font-black px-2.5 py-1.5 rounded-md bg-surface-container-low"
                >
                  Edit
                </RouterLink>
                <button
                  type="button"
                  class="text-[11px] font-black px-2.5 py-1.5 rounded-md bg-error/10 text-error"
                  @click="openDeleteConfirm(slot)"
                >
                  Delete
                </button>
              </div>
            </div>
          </div>
          <p v-if="!isLoading && selectedDaySlots.length === 0" class="text-sm text-on-surface-variant">
            No timeslots for this day yet.
          </p>
          <p class="text-xs text-on-surface-variant mt-4">Select a day in the calendar and use the plus button to add a timeslot to that exact date.</p>
        </div>
          </div>

      <ConfirmDialog
        :is-open="showDeleteConfirm"
        title="Delete Timeslot?"
        @cancel="cancelDelete"
        @confirm="confirmDelete"
      >
        Are you sure you want to delete <strong>{{ slotToDelete?.title || 'this timeslot' }}</strong>
        <span v-if="slotToDelete">({{ formatPeriod(slotToDelete.startTime, slotToDelete.durationMinutes) }})</span>?
        This action cannot be undone.
      </ConfirmDialog>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppSidebar from '../components/AppSidebar.vue';
import ConfirmDialog from '../components/ConfirmDialog.vue';
import MainTopBar from '../components/MainTopBar.vue';
import { deleteProjectTimeslot, getProjectCalendar } from '../services/apiClient';
import { formatHoursToHM } from '../utils/timeFormatter';

/**
 * Calendar detail page for a specific project.
 */
const route = useRoute();
const router = useRouter();
const projectId = computed(() => String(route.params.projectId || 'math-grade-10'));
const projectName = ref('Math Grade 10');
const totalHours = ref(0);
const monthHours = ref(0);
const monthSlots = ref([]);
const isLoading = ref(false);
const errorMessage = ref('');
const searchText = ref('');
const activeMonth = ref(parseMonthFromQuery(route.query.month));
const selectedDate = ref('');
const showDeleteConfirm = ref(false);
const slotToDelete = ref(null);

const weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const monthKey = computed(() => {
  const year = activeMonth.value.getFullYear();
  const month = String(activeMonth.value.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
});
const monthLabel = computed(() => new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(activeMonth.value));
const calendarCells = computed(() => buildCalendarCells(activeMonth.value));

const formattedTotalHours = computed(() => {
  return formatHoursToHM(totalHours.value);
});

const formattedMonthHours = computed(() => {
  return formatHoursToHM(monthHours.value);
});

const slotCountByDate = computed(() => {
  return monthSlots.value.reduce((acc, slot) => {
    acc[slot.date] = (acc[slot.date] || 0) + 1;
    return acc;
  }, {});
});

const selectedDaySlots = computed(() => {
  if (!selectedDate.value) {
    return [];
  }
  return monthSlots.value
    .filter((slot) => slot.date === selectedDate.value)
    .sort((a, b) => a.startTime.localeCompare(b.startTime));
});

const hasActiveSearch = computed(() => searchText.value.trim().length > 0);

const filteredSlotResults = computed(() => {
  const query = searchText.value.toLowerCase().trim();
  if (!query) {
    return monthSlots.value;
  }

  return monthSlots.value
    .filter((slot) => {
      const haystack = [slot.title, slot.description, slot.date, slot.startTime]
        .map((item) => String(item || '').toLowerCase())
        .join(' ');
      return haystack.includes(query);
    })
    .sort((left, right) => {
      const leftKey = `${left.date} ${left.startTime}`;
      const rightKey = `${right.date} ${right.startTime}`;
      return leftKey.localeCompare(rightKey);
    });
});

const selectedDayTitle = computed(() => {
  if (!selectedDate.value) {
    return 'Select Day';
  }
  return new Intl.DateTimeFormat('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric'
  }).format(new Date(`${selectedDate.value}T00:00:00`));
});

async function loadCalendar() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const payload = await getProjectCalendar(projectId.value, monthKey.value);
    projectName.value = payload.projectName;
    totalHours.value = payload.totalHours;
    monthHours.value = payload.monthHours;
    monthSlots.value = payload.monthSlots || [];

    if (!selectedDate.value || !selectedDate.value.startsWith(monthKey.value)) {
      const todayIso = getTodayIso();
      selectedDate.value = todayIso.startsWith(monthKey.value) ? todayIso : `${monthKey.value}-01`;
    }
  } catch (error) {
    console.warn('Using fallback calendar data because API is unavailable.', error);
    errorMessage.value = error.message;
    projectName.value = 'Project Calendar';
    totalHours.value = 0;
    monthHours.value = 0;
    monthSlots.value = [];
  } finally {
    isLoading.value = false;
  }
}

function getTodayIso() {
  return new Date().toISOString().slice(0, 10);
}

function selectDay(dateIso, inMonth) {
  if (!inMonth) {
    return;
  }
  selectedDate.value = dateIso;
}

function goToPreviousMonth() {
  const next = new Date(activeMonth.value);
  next.setMonth(next.getMonth() - 1);
  activeMonth.value = next;
}

function goToNextMonth() {
  const next = new Date(activeMonth.value);
  next.setMonth(next.getMonth() + 1);
  activeMonth.value = next;
}

function formatTime(timeValue) {
  const raw = String(timeValue || '00:00').slice(0, 5);
  const [hours, minutes] = raw.split(':').map(Number);
  const safeHours = Number.isNaN(hours) ? 0 : hours;
  const safeMinutes = Number.isNaN(minutes) ? 0 : minutes;
  const d = new Date(2000, 0, 1, safeHours, safeMinutes);
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit' }).format(d);
}

function formatPeriod(startTime, durationMinutes) {
  const raw = String(startTime || '00:00').slice(0, 5);
  const [hours, minutes] = raw.split(':').map(Number);
  const safeHours = Number.isNaN(hours) ? 0 : hours;
  const safeMinutes = Number.isNaN(minutes) ? 0 : minutes;
  const start = new Date(2000, 0, 1, safeHours, safeMinutes);
  const end = new Date(start.getTime() + (Number(durationMinutes) || 0) * 60000);
  const formatter = new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit' });
  return `${formatter.format(start)} - ${formatter.format(end)} (${Number(durationMinutes) || 0} min)`;
}

function openDeleteConfirm(slot) {
  slotToDelete.value = slot;
  showDeleteConfirm.value = true;
}

function cancelDelete() {
  showDeleteConfirm.value = false;
  slotToDelete.value = null;
}

async function confirmDelete() {
  if (!slotToDelete.value?.id) {
    return;
  }

  try {
    await deleteProjectTimeslot(projectId.value, slotToDelete.value.id);
    await loadCalendar();
    cancelDelete();
  } catch (error) {
    errorMessage.value = error.message;
  }
}

function parseMonthFromQuery(monthQuery) {
  if (typeof monthQuery === 'string' && /^\d{4}-\d{2}$/.test(monthQuery)) {
    return new Date(`${monthQuery}-01T00:00:00`);
  }
  return new Date();
}

function buildCalendarCells(monthDate) {
  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();
  const firstOfMonth = new Date(year, month, 1);
  const weekday = firstOfMonth.getDay();
  const mondayStartOffset = (weekday + 6) % 7;
  const startDate = new Date(year, month, 1 - mondayStartOffset);

  return Array.from({ length: 42 }, (_, index) => {
    const d = new Date(startDate);
    d.setDate(startDate.getDate() + index);
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    return {
      key: iso,
      day: d.getDate(),
      dateIso: iso,
      inMonth: d.getMonth() === month
    };
  });
}

watch([projectId, monthKey], () => {
  router.replace({
    name: 'project-calendar',
    params: { projectId: projectId.value },
    query: { month: monthKey.value }
  });
  loadCalendar();
}, { immediate: true });
</script>
