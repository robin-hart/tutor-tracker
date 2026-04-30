<template>
  <aside class="fixed left-0 top-0 h-screen w-72 bg-[#eff4ff] flex flex-col p-6 space-y-8 z-50">
    <div class="flex items-center gap-3 px-2">
      <div class="w-10 h-10 bg-primary rounded-lg flex items-center justify-center text-white">
        <span class="material-symbols-outlined">menu_book</span>
      </div>
      <div>
        <h1 class="text-2xl font-black text-[#24389c] font-headline">TutorFlow</h1>
        <p class="text-[10px] uppercase tracking-widest text-on-surface-variant font-bold">
          Premium Tracking
        </p>
      </div>
    </div>
    <nav class="flex-1 space-y-2">
      <RouterLink
        :to="{ name: 'projects-dashboard' }"
        class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all hover:translate-x-1"
        :class="
          isProjectSectionActive
            ? 'bg-white text-primary font-bold'
            : 'text-on-surface-variant hover:bg-surface-container-highest'
        "
      >
        <span class="material-symbols-outlined">folder_shared</span>
        <span class="font-manrope font-semibold tracking-tight">Projects</span>
      </RouterLink>
      <RouterLink
        :to="{ name: 'export-reports' }"
        class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all hover:translate-x-1"
        :class="
          isActive('export-reports')
            ? 'bg-white text-primary font-bold'
            : 'text-on-surface-variant hover:bg-surface-container-highest'
        "
      >
        <span class="material-symbols-outlined">analytics</span>
        <span class="font-manrope font-semibold tracking-tight">Reports</span>
      </RouterLink>
        <RouterLink
          :to="{ name: 'settings' }"
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-all hover:translate-x-1"
          :class="
            isActive('settings')
              ? 'bg-white text-primary font-bold'
              : 'text-on-surface-variant hover:bg-surface-container-highest'
          "
        >
          <span class="material-symbols-outlined">settings</span>
          <span class="font-manrope font-semibold tracking-tight">Settings</span>
        </RouterLink>
      </nav>
    <RouterLink
      :to="{ name: 'projects-dashboard' }"
      class="premium-gradient text-on-primary py-3 px-4 rounded-lg font-headline font-bold shadow-lg text-center"
    >
      Open Projects
    </RouterLink>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

/**
 * Shared application sidebar used across all main views.
 */
const route = useRoute();

const projectSectionRouteNames = new Set([
  'projects-dashboard',
  'project-calendar',
  'student-management',
  'timeslot-editor',
]);
const isProjectSectionActive = computed(() =>
  projectSectionRouteNames.has(String(route.name || ''))
);

/**
 * Determines whether a route name is currently active.
 *
 * @param {string} name route name
 * @returns {boolean}
 */
function isActive(name: string): boolean {
  return route.name === name;
}
</script>
