<template>
  <div class="student-management-view bg-surface text-on-surface min-h-screen">
    <AppSidebar />
    <MainTopBar search-placeholder="Search students...">
      <template #tabs>
        <nav class="flex gap-6">
          <RouterLink :to="{ name: 'project-calendar', params: { projectId } }" class="text-on-surface-variant font-manrope font-medium text-sm">Calendar</RouterLink>
          <a class="text-primary font-bold border-b-2 border-primary pb-1 font-manrope text-sm">Students</a>
        </nav>
      </template>
    </MainTopBar>

    <main class="ml-72 pt-32 px-12 pb-20">
      <div class="flex items-end justify-between mb-12">
        <div>
          <h2 class="text-5xl font-extrabold tracking-tighter">Manage Students</h2>
        </div>
        <button @click="showAddStudent = !showAddStudent" class="flex items-center gap-3 bg-white border-2 border-primary text-primary px-8 py-4 rounded-2xl font-bold">Add New Student</button>
      </div>

      <section class="space-y-8">
        <div v-if="showAddStudent" class="bg-surface-container-lowest rounded-xl p-6 grid grid-cols-1 md:grid-cols-4 gap-3 items-end">
          <input v-model.trim="newStudent.name" class="bg-surface-container-low rounded-lg px-3 py-2" placeholder="Name" type="text" />
          <input v-model.trim="newStudent.notes" class="bg-surface-container-low rounded-lg px-3 py-2" placeholder="Initial notes (optional)" type="text" />
          <select v-model="newStudent.groupName" class="bg-surface-container-low rounded-lg px-3 py-2">
            <option value="">Ungrouped (default)</option>
            <option v-for="group in selectableGroups" :key="group" :value="group">{{ group }}</option>
          </select>
          <button @click="addStudent" class="bg-primary text-white rounded-lg px-4 py-2 font-bold">Add</button>
        </div>

        <div class="max-w-md">
          <input
            v-model.trim="searchText"
            class="w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20"
            placeholder="Filter students by name"
            type="text"
          />
        </div>

        <p v-if="isLoading" class="text-on-surface-variant">Loading students...</p>
        <p v-if="errorMessage" class="text-error">{{ errorMessage }}</p>

        <div
          ref="groupScrollContainer"
          class="space-y-10"
          :class="isDragging ? 'max-h-[62vh] overflow-y-auto pr-2' : ''"
        >
          <section
            v-for="group in groupedStudents"
            :key="group.name"
            :data-group-name="group.name"
            class="bg-surface-container-lowest rounded-2xl p-6 transition-all duration-200"
            :class="[
              isDragging ? 'py-3' : '',
              hoveredGroupName === group.name ? 'ring-2 ring-primary/50' : ''
            ]"
          >
            <div class="flex items-center justify-between gap-4 mb-5">
              <div class="flex items-center gap-4">
                <div class="h-[2px] w-8 bg-primary-container/30"></div>
                <h3 class="text-xl font-bold text-on-surface-variant">{{ group.name }}</h3>
                <span class="text-xs px-2 py-1 rounded-md bg-primary/10 text-primary font-bold">{{ group.students.length }}</span>
              </div>
              <button
                v-if="group.name !== 'Ungrouped'"
                type="button"
                class="text-xs font-bold text-error bg-error/10 px-3 py-1.5 rounded-md"
                @click="removeGroup(group.name)"
              >
                Delete Group
              </button>
            </div>

            <div
              class="grid grid-cols-1 xl:grid-cols-2 gap-6 min-h-20"
              :class="isDragging ? 'min-h-12' : ''"
            >
              <article
                v-for="student in group.students"
                :key="student.id"
                class="bg-white p-6 rounded-2xl shadow-[0_20px_40px_rgba(11,28,48,0.03)]"
                :class="[
                  isDragging && draggedStudentId !== student.id ? 'hidden' : '',
                  draggedStudentId === student.id ? 'opacity-60' : ''
                ]"
              >
                <div class="flex justify-between items-start">
                  <div>
                    <h4 class="text-lg font-bold">{{ student.name }}</h4>
                    <p class="text-sm text-on-surface-variant">Last active: {{ student.lastActive }}</p>
                  </div>
                  <button
                    type="button"
                    class="drag-handle h-8 w-8 rounded-md bg-surface-container-low text-on-surface-variant flex items-center justify-center"
                    @mousedown.left.prevent="onDragStart(student.id, $event)"
                    title="Drag student"
                  >
                    <span class="material-symbols-outlined text-base">drag_indicator</span>
                  </button>
                </div>
                <div class="relative mt-5">
                  <p class="absolute -top-3 left-4 px-2 bg-white text-[10px] font-black uppercase tracking-widest text-primary">Progress Notes</p>
                  <textarea
                    class="w-full h-28 bg-surface-container-low border-none rounded-2xl p-4 focus:ring-2 ring-primary/20 resize-none"
                    v-model="student.notes"
                    draggable="false"
                    @dragstart.prevent
                  />
                </div>
                <div class="mt-3 flex justify-end">
                  <button @click="saveNotes(student)" class="text-sm font-bold text-primary">Save Notes</button>
                </div>
              </article>

              <div v-if="isDragging" class="col-span-full border-2 border-dashed border-primary/40 rounded-xl py-3 px-4 text-sm text-on-surface-variant bg-white/50">
                Drop here to move student to <span class="font-bold text-primary">{{ group.name }}</span>
              </div>
            </div>
          </section>

          <p v-if="groupedStudents.length === 0" class="text-on-surface-variant">No students found for this filter.</p>
        </div>

        <div class="pt-6 pb-4 flex flex-col items-center gap-3">
          <button
            type="button"
            class="h-12 w-12 rounded-full bg-primary text-white shadow-lg flex items-center justify-center"
            @click="showAddGroup = !showAddGroup"
            title="Add group"
          >
            <span class="material-symbols-outlined">add</span>
          </button>

          <div v-if="showAddGroup" class="w-full max-w-md bg-white rounded-xl border border-outline-variant/30 shadow-xl p-4 space-y-3">
            <p class="text-sm font-black">Create New Group</p>
            <input v-model.trim="newGroupName" class="w-full bg-surface-container-low rounded-lg px-3 py-2" placeholder="e.g. Group C" type="text" />
            <div class="flex justify-end gap-2">
              <button class="px-3 py-2 text-sm font-bold text-on-surface-variant" @click="showAddGroup = false">Cancel</button>
              <button class="px-3 py-2 text-sm font-bold rounded-lg bg-primary text-white" @click="addGroup">Add Group</button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import AppSidebar from '../components/AppSidebar.vue';
import MainTopBar from '../components/MainTopBar.vue';
import {
  createProjectGroup,
  createProjectStudent,
  deleteProjectGroup,
  getProjectGroups,
  getProjectStudents,
  updateStudentGroup,
  updateStudentNotes
} from '../services/apiClient';

const route = useRoute();
const projectId = computed(() => String(route.params.projectId || 'math-grade-10'));
const students = ref([]);
const groups = ref(['Ungrouped']);
const isLoading = ref(false);
const errorMessage = ref('');
const searchText = ref('');
const showAddStudent = ref(false);
const showAddGroup = ref(false);
const newGroupName = ref('');
const draggedStudentId = ref('');
const isDragging = ref(false);
const hoveredGroupName = ref('');
const groupScrollContainer = ref(null);
const newStudent = ref({
  name: '',
  notes: '',
  groupName: ''
});

const filteredStudents = computed(() => {
  const query = searchText.value.toLowerCase();
  if (!query) {
    return students.value;
  }
  return students.value.filter((student) => student.name.toLowerCase().includes(query));
});

const selectableGroups = computed(() => groups.value.filter((group) => group !== 'Ungrouped'));

const groupedStudents = computed(() => {
  const byGroup = new Map(groups.value.map((groupName) => [groupName, []]));

  for (const student of filteredStudents.value) {
    const groupName = normalizeGroupName(student.groupName);
    if (!byGroup.has(groupName)) {
      byGroup.set(groupName, []);
    }
    byGroup.get(groupName).push(student);
  }

  return Array.from(byGroup.entries())
    .sort(([left], [right]) => {
      if (left === 'Ungrouped') {
        return -1;
      }
      if (right === 'Ungrouped') {
        return 1;
      }
      return left.localeCompare(right);
    })
    .map(([name, groupStudents]) => ({ name, students: groupStudents }));
});

function normalizeGroupName(groupName) {
  if (!groupName || !String(groupName).trim()) {
    return 'Ungrouped';
  }
  return String(groupName).trim();
}

async function loadStudentContext() {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const [loadedStudents, loadedGroups] = await Promise.all([
      getProjectStudents(projectId.value),
      getProjectGroups(projectId.value)
    ]);

    students.value = loadedStudents.map((student) => ({
      ...student,
      groupName: normalizeGroupName(student.groupName)
    }));

    const groupNames = new Set(['Ungrouped']);
    for (const group of loadedGroups) {
      groupNames.add(normalizeGroupName(group.name));
    }
    for (const student of students.value) {
      groupNames.add(normalizeGroupName(student.groupName));
    }
    groups.value = Array.from(groupNames);
  } catch (error) {
    errorMessage.value = error.message;
    students.value = [
      {
        id: 'alex-thompson',
        name: 'Alex Thompson',
        lastActive: '2 hours ago',
        notes: 'Struggling with quadratic equations. Requires focus on discriminant formula next session.',
        groupName: 'Group A'
      },
      {
        id: 'maya-rodriguez',
        name: 'Maya Rodriguez',
        lastActive: 'Yesterday',
        notes: 'Excellent grasp of trigonometry. Ready for advanced circle theorem exercises.',
        groupName: 'Ungrouped'
      }
    ];
    groups.value = ['Ungrouped', 'Group A'];
  } finally {
    isLoading.value = false;
  }
}

async function addGroup() {
  if (!newGroupName.value.trim()) {
    return;
  }

  const groupName = normalizeGroupName(newGroupName.value);
  if (groupName === 'Ungrouped') {
    newGroupName.value = '';
    showAddGroup.value = false;
    return;
  }

  try {
    const created = await createProjectGroup(projectId.value, groupName);
    if (!groups.value.includes(created.name)) {
      groups.value = [...groups.value, created.name];
    }
    newGroupName.value = '';
    showAddGroup.value = false;
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function removeGroup(groupName) {
  if (groupName === 'Ungrouped') {
    return;
  }

  try {
    await deleteProjectGroup(projectId.value, groupName);
    groups.value = groups.value.filter((group) => group !== groupName);
    students.value = students.value.map((student) => {
      if (normalizeGroupName(student.groupName) === groupName) {
        return { ...student, groupName: 'Ungrouped' };
      }
      return student;
    });
  } catch (error) {
    errorMessage.value = error.message;
  }
}

async function addStudent() {
  if (!newStudent.value.name) {
    return;
  }

  const selectedGroup = normalizeGroupName(newStudent.value.groupName);
  try {
    const created = await createProjectStudent(projectId.value, {
      ...newStudent.value,
      groupName: selectedGroup
    });
    students.value = [{ ...created, groupName: normalizeGroupName(created.groupName) }, ...students.value];
    if (!groups.value.includes(selectedGroup)) {
      groups.value = [...groups.value, selectedGroup];
    }
    newStudent.value = { name: '', notes: '', groupName: '' };
    showAddStudent.value = false;
  } catch (error) {
    errorMessage.value = error.message;
  }
}

function onDragStart(studentId, event) {
  if (event.button !== 0) {
    return;
  }

  draggedStudentId.value = studentId;
  isDragging.value = true;
  hoveredGroupName.value = '';

  globalThis.document.body.style.userSelect = 'none';
  globalThis.addEventListener('mousemove', onPointerMove);
  globalThis.addEventListener('mouseup', onGlobalMouseUp);
}

function onDragEnd() {
  globalThis.document.body.style.userSelect = '';
  globalThis.removeEventListener('mousemove', onPointerMove);
  globalThis.removeEventListener('mouseup', onGlobalMouseUp);
  draggedStudentId.value = '';
  hoveredGroupName.value = '';
  isDragging.value = false;
}

function onPointerMove(event) {
  if (!isDragging.value || !groupScrollContainer.value) {
    return;
  }

  const container = groupScrollContainer.value;
  const bounds = container.getBoundingClientRect();
  const threshold = 80;
  const step = 16;

  if (event.clientY < bounds.top + threshold) {
    container.scrollTop -= step;
  } else if (event.clientY > bounds.bottom - threshold) {
    container.scrollTop += step;
  }

  const element = globalThis.document.elementFromPoint(event.clientX, event.clientY);
  const targetGroupElement = element ? element.closest('[data-group-name]') : null;
  hoveredGroupName.value = targetGroupElement?.dataset?.groupName || '';
}

function onGlobalMouseUp() {
  if (!isDragging.value) {
    return;
  }

  const targetGroup = hoveredGroupName.value;
  if (targetGroup) {
    void moveStudentToGroup(targetGroup);
  }

  onDragEnd();
}

async function moveStudentToGroup(groupName) {
  if (!draggedStudentId.value) {
    return;
  }

  const student = students.value.find((candidate) => candidate.id === draggedStudentId.value);
  if (!student) {
    return;
  }

  const targetGroup = normalizeGroupName(groupName);
  const previousGroup = normalizeGroupName(student.groupName);
  if (targetGroup === previousGroup) {
    return;
  }

  student.groupName = targetGroup;
  try {
    await updateStudentGroup(student.id, targetGroup);
  } catch (error) {
    student.groupName = previousGroup;
    errorMessage.value = error.message;
  }
}

async function saveNotes(student) {
  try {
    await updateStudentNotes(student.id, student.notes);
  } catch (error) {
    errorMessage.value = error.message;
  }
}

watch(projectId, () => {
  loadStudentContext();
}, { immediate: true });

onBeforeUnmount(() => {
  globalThis.document.body.style.userSelect = '';
  globalThis.removeEventListener('mousemove', onPointerMove);
  globalThis.removeEventListener('mouseup', onGlobalMouseUp);
});
</script>

<style scoped>
.student-management-view :deep(a),
.student-management-view :deep(button) {
  cursor: default;
}

.student-management-view :deep(input),
.student-management-view :deep(textarea),
.student-management-view :deep(select) {
  cursor: text;
}

.student-management-view :deep(.drag-handle) {
  cursor: grab;
}

.student-management-view :deep(.drag-handle:active) {
  cursor: grabbing;
}
</style>
