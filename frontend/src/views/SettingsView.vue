<template>
  <div class="text-on-surface bg-background min-h-screen">
    <AppSidebar />
    <MainTopBar search-placeholder="Search settings...">
      <template #tabs>
        <nav class="flex items-center gap-6">
          <a class="text-primary font-bold border-b-2 border-primary pb-1 font-manrope text-sm"
            >Profile</a
          >
        </nav>
      </template>
    </MainTopBar>

    <main class="ml-72 pt-32 px-12 pb-20">
      <div class="mb-12">
        <h1 class="font-headline text-5xl font-extrabold tracking-tight mb-2">Settings</h1>
        <p class="text-on-surface-variant text-lg max-w-2xl">
          Configure your personal settings to customize your reports and documentation.
        </p>
      </div>

      <section class="max-w-2xl">
        <div
          class="bg-surface-container-low rounded-2xl overflow-hidden shadow-[0_20px_40px_rgba(11,28,48,0.04)] p-8"
        >
          <div class="mb-8">
            <h2 class="font-headline font-bold text-2xl mb-1">Your Name</h2>
            <p class="text-on-surface-variant text-sm">
              Your name will be used in generated reports and documentation. Leave empty to omit
              from reports.
            </p>
          </div>

          <div class="mb-8">
            <label class="block">
              <span class="text-sm font-semibold text-on-surface-variant uppercase tracking-widest"
                >Full Name</span
              >
              <input
                v-model="localUserName"
                type="text"
                placeholder="Enter your full name"
                class="mt-3 w-full bg-surface-container-lowest rounded-xl px-4 py-3 border-none focus:ring-2 focus:ring-primary/20 font-body text-sm"
                @keyup.enter="saveName"
              />
            </label>
            <p v-if="localUserName.length > 0" class="text-xs text-on-surface-variant mt-2">
              This name will appear in your reports as the creator/responsible person.
            </p>
          </div>

          <div class="flex gap-3">
            <button
              class="premium-gradient text-on-primary px-6 py-3 rounded-lg font-bold text-sm transition-all hover:shadow-lg"
              @click="saveName"
            >
              Save Name
            </button>
            <button
              v-if="localUserName.length > 0"
              class="bg-surface-container-highest text-on-surface px-6 py-3 rounded-lg font-bold text-sm transition-all hover:bg-surface-container-high"
              @click="clearName"
            >
              Clear Name
            </button>
          </div>

          <div v-if="showSuccessMessage" class="mt-6 p-4 bg-success-container rounded-xl">
            <p class="text-on-success-container font-semibold">✓ Name saved successfully</p>
          </div>
        </div>

        <div
          class="mt-8 bg-surface-container-low rounded-2xl overflow-hidden shadow-[0_20px_40px_rgba(11,28,48,0.04)] p-8"
        >
          <h2 class="font-headline font-bold text-2xl mb-4">About Your Settings</h2>
          <ul class="space-y-3 text-on-surface-variant text-sm">
            <li class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary flex-shrink-0 mt-0.5"
                >check_circle</span
              >
              <span>Your settings are stored locally in your browser</span>
            </li>
            <li class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary flex-shrink-0 mt-0.5"
                >check_circle</span
              >
              <span>Your name is used when generating PDF reports</span>
            </li>
            <li class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary flex-shrink-0 mt-0.5"
                >check_circle</span
              >
              <span>Clearing your name will remove it from future reports</span>
            </li>
          </ul>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import AppSidebar from '../components/AppSidebar.vue';
import MainTopBar from '../components/MainTopBar.vue';
import { useUserSettings } from '../composables/useUserSettings';

/**
 * Settings view for user profile and preferences.
 */
const { userName, saveUserName, clearUserName } = useUserSettings();
const localUserName = ref('');
const showSuccessMessage = ref(false);

onMounted(() => {
  localUserName.value = userName.value;
});

function saveName(): void {
  saveUserName(localUserName.value);
  showSuccessMessage.value = true;
  setTimeout(() => {
    showSuccessMessage.value = false;
  }, 3000);
}

function clearName(): void {
  localUserName.value = '';
  clearUserName();
  showSuccessMessage.value = true;
  setTimeout(() => {
    showSuccessMessage.value = false;
  }, 3000);
}
</script>
