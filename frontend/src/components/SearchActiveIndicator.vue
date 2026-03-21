<template>
  <section
    class="bg-primary/5 border border-primary/20 rounded-xl px-4 py-3 flex items-center justify-between gap-3"
  >
    <p class="text-sm text-on-surface-variant">
      Search active for <span class="font-bold text-on-surface">"{{ query }}"</span>.
      Showing <span class="font-bold text-primary">{{ shownCount }}</span>
      of <span class="font-bold text-on-surface">{{ totalCount }}</span> {{ entityLabel }}.
    </p>
    <button
      type="button"
      class="text-xs font-bold px-3 py-1.5 rounded-md bg-surface-container-low text-on-surface"
      @click="emit('clear')"
    >
      Clear
    </button>
  </section>
</template>

<script setup>
defineProps({
  query: {
    type: String,
    default: ''
  },
  shownCount: {
    type: Number,
    default: 0
  },
  totalCount: {
    type: Number,
    default: 0
  },
  entityLabel: {
    type: String,
    default: 'items'
  }
});

const emit = defineEmits(['clear']);
</script>
