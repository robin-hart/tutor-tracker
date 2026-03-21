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
        <button @click="onCancel" class="px-4 py-2 rounded-lg bg-surface-container-low text-on-surface font-bold">{{ cancelLabel }}</button>
        <button @click="onConfirm" class="px-4 py-2 rounded-lg bg-error text-on-error font-bold">{{ confirmLabel }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted } from 'vue';

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: 'Confirm Action'
  },
  confirmLabel: {
    type: String,
    default: 'Delete'
  },
  cancelLabel: {
    type: String,
    default: 'Cancel'
  }
});

const emit = defineEmits(['confirm', 'cancel']);

function onConfirm() {
  emit('confirm');
}

function onCancel() {
  emit('cancel');
}

function onEscapeKey(event) {
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