<template>
  <div
    v-if="isOpen"
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
    @click.self="onCancel"
  >
    <div class="bg-surface-container-highest rounded-xl shadow-2xl p-8 max-w-md">
      <h2 class="text-2xl font-bold font-headline mb-4">{{ title }}</h2>
      <p class="text-on-surface-variant mb-6">
        <slot />
      </p>
      <div class="flex gap-3 justify-end">
        <button
          class="px-4 py-2 rounded-lg bg-surface-container-low text-on-surface font-bold"
          @click="onCancel"
        >
          {{ cancelLabel }}
        </button>
        <button class="px-4 py-2 rounded-lg bg-error text-on-error font-bold" @click="onConfirm">
          {{ confirmLabel }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue';

const props = withDefaults(
  defineProps<{
    isOpen?: boolean;
    title?: string;
    confirmLabel?: string;
    cancelLabel?: string;
  }>(),
  {
    isOpen: false,
    title: 'Confirm Action',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
  }
);

const emit = defineEmits<{
  (event: 'confirm'): void;
  (event: 'cancel'): void;
}>();

/**
 * Emits a confirmation action to the parent.
 */
function onConfirm(): void {
  emit('confirm');
}

/**
 * Emits a cancel action to the parent.
 */
function onCancel(): void {
  emit('cancel');
}

/**
 * Handles Escape key presses when the dialog is open.
 */
function onEscapeKey(event: KeyboardEvent): void {
  if (event.key === 'Escape' && props.isOpen) {
    onCancel();
  }
}

onMounted(() => {
  window.addEventListener('keydown', onEscapeKey);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onEscapeKey);
});
</script>
