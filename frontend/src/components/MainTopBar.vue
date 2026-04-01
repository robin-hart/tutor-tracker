<template>
  <header
    class="fixed top-0 right-0 left-72 h-20 z-40 bg-[#f8f9ff]/80 backdrop-blur-xl flex items-center justify-between px-10 w-full shadow-sm"
  >
    <div class="flex items-center gap-8">
      <div class="relative">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant"
          >search</span
        >
        <input
          class="pl-10 pr-10 py-2 bg-surface-container-highest border-none rounded-full w-64 focus:ring-2 focus:ring-primary/20 focus:bg-surface-bright transition-all text-sm font-body"
          :placeholder="searchPlaceholder"
          type="text"
          :value="modelValue"
          :disabled="searchDisabled"
          @input="onSearchInput"
        />
        <button
          v-if="modelValue && !searchDisabled"
          type="button"
          class="absolute right-2 top-1/2 -translate-y-1/2 h-6 w-6 rounded-full bg-surface-container-low text-on-surface-variant hover:bg-surface-container-high flex items-center justify-center"
          aria-label="Clear search"
          @click="clearSearch"
        >
          <span class="material-symbols-outlined text-sm">close</span>
        </button>
      </div>
      <slot name="tabs" />
    </div>
    <div class="flex items-center gap-4">
      <button
        class="bg-secondary-container text-on-secondary-container px-5 py-2 rounded-full font-headline font-bold text-sm hover:opacity-90 active:scale-95 transition-all"
      >
        Start Timer
      </button>
    </div>
  </header>
</template>

<script setup>
/**
 * Shared top bar component with search and pluggable tab content.
 */
defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  searchPlaceholder: {
    type: String,
    default: 'Search...',
  },
  searchDisabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:modelValue']);

function onSearchInput(event) {
  emit('update:modelValue', event.target.value);
}

function clearSearch() {
  emit('update:modelValue', '');
}
</script>
