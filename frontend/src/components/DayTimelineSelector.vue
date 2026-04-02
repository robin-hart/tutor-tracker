<template>
  <section
    ref="rootEl"
    class="rounded-2xl border border-outline-variant bg-surface-container-low overflow-hidden select-none"
  >
    <header class="px-4 py-3 border-b border-outline-variant bg-surface-container-high">
      <p class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">Time</p>
      <p class="text-sm font-semibold text-on-surface">{{ startTime }} - {{ endTime }}</p>
    </header>

    <div
      ref="scrollHost"
      class="relative h-[26rem] overflow-y-auto timeline-scroll"
      @click="onTimelineClick"
    >
      <div class="relative" :style="{ height: timelineHeightPx + 'px' }">
        <div
          v-for="hour in visibleHours"
          :key="hour"
          class="absolute left-0 right-0 border-t border-outline-variant/40"
          :style="{ top: minuteToPixel(hour * 60) + 'px' }"
        >
          <span
            class="absolute left-3 bg-surface-container-low px-1 text-[11px] font-medium text-on-surface-variant"
            :style="{ top: hour === dayStartHour ? '2px' : hour === dayEndHour ? '-14px' : '-8px' }"
          >
            {{ String(hour).padStart(2, '0') }}:00
          </span>
        </div>

        <div
          class="absolute left-14 right-3 rounded-xl border-2 border-primary bg-primary/20 shadow-sm"
          :style="selectionStyle"
          @mousedown.stop="startDrag"
        >
          <div
            class="absolute inset-x-0 -top-2 h-4 cursor-ns-resize"
            @mousedown.stop="startResize('start', $event)"
          ></div>
          <div
            class="absolute inset-x-0 -bottom-2 h-4 cursor-ns-resize"
            @mousedown.stop="startResize('end', $event)"
          ></div>
          <div class="h-full flex items-center justify-center px-2 text-xs font-semibold text-primary gap-2">
            <span>{{ durationLabel }}</span>
            <button
              type="button"
              class="rounded-md border border-primary/40 bg-white/70 px-2 py-1 text-[11px] font-bold text-primary hover:bg-white"
              @click.stop="toggleDurationMenu"
            >
              Edit
            </button>
          </div>

          <div
            v-if="showDurationMenu"
            class="absolute left-1/2 top-1/2 z-20 w-36 -translate-x-1/2 -translate-y-1/2 rounded-lg border border-outline-variant bg-surface shadow-xl"
            @mousedown.stop
          >
            <button
              type="button"
              class="absolute -top-6 left-1/2 inline-flex h-7 w-7 -translate-x-1/2 items-center justify-center rounded-full border border-primary/50 bg-primary text-white shadow-md hover:bg-primary-container"
              aria-label="Close duration selector"
              @click.stop="showDurationMenu = false"
            >
              <span class="material-symbols-outlined text-[16px] leading-none">close</span>
            </button>
            <ul class="max-h-36 overflow-y-auto py-2 timeline-scroll">
              <li v-for="option in durationOptions" :key="option">
                <button
                  type="button"
                  class="block w-full px-3 py-2 text-left text-xs text-on-surface hover:bg-surface-container-low"
                  @click.stop="selectDuration(option)"
                >
                  {{ formatDuration(option) }}
                </button>
              </li>
            </ul>
          </div>
        </div>

        <div
          class="absolute left-2 rounded-md bg-primary px-2 py-1 text-[11px] font-bold text-on-primary shadow"
          :style="startBadgeStyle"
        >
          {{ startTime }}
        </div>

        <div
          class="absolute left-2 rounded-md bg-primary px-2 py-1 text-[11px] font-bold text-on-primary shadow"
          :style="endBadgeStyle"
        >
          {{ endTime }}
        </div>
      </div>
    </div>

  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  startTime: {
    type: String,
    required: true,
  },
  durationMinutes: {
    type: Number,
    required: true,
  },
});

const emit = defineEmits(['update:startTime', 'update:durationMinutes']);

const dayStartHour = 0;
const dayEndHour = 24;
const pixelsPerMinute = 1.2;
const stepMinutes = 15;
const minDuration = 15;
const maxDuration = 6 * 60;
const dayStartMinute = dayStartHour * 60;
const dayEndMinute = dayEndHour * 60 - stepMinutes;
const dayEndBoundaryMinute = dayEndHour * 60;

const durationOptions = Array.from({ length: maxDuration / stepMinutes }, (_, i) => (i + 1) * stepMinutes);
const visibleHours = Array.from({ length: dayEndHour - dayStartHour + 1 }, (_, i) => i + dayStartHour);
const timelineHeightPx = (dayEndHour - dayStartHour) * 60 * pixelsPerMinute;

const startMinutes = computed(() => parseTime(props.startTime));
const endMinutes = computed(() => clampEndMinute(startMinutes.value + props.durationMinutes));
const endTime = computed(() => toTime(endMinutes.value));
const durationLabel = computed(() => formatDuration(props.durationMinutes));

const selectionStyle = computed(() => ({
  top: `${minuteToPixel(startMinutes.value)}px`,
  height: `${Math.max(minDuration * pixelsPerMinute, props.durationMinutes * pixelsPerMinute)}px`,
  cursor: dragState.value.mode ? 'grabbing' : 'grab',
}));

const startBadgeStyle = computed(() => ({
  left: '3.25rem',
  top: `${minuteToPixel(startMinutes.value)}px`,
  transform:
    startMinutes.value === dayStartMinute
      ? 'translate(-100%, 0)'
      : 'translate(-100%, -50%)',
}));

const endBadgeStyle = computed(() => ({
  left: '3.25rem',
  top: `${minuteToPixel(endMinutes.value)}px`,
  transform:
    endMinutes.value >= dayEndBoundaryMinute
      ? 'translate(-100%, -100%)'
      : 'translate(-100%, -50%)',
}));

const scrollHost = ref(null);
const rootEl = ref(null);
const dragState = ref({
  mode: null,
  baseStart: 0,
  baseDuration: 0,
  pointerOffsetMinutes: 0,
  moved: false,
});
const showDurationMenu = ref(false);
const suppressNextClick = ref(false);
const dragPointerClientY = ref(null);
const dragLoopFrame = ref(0);
const hasUserInteraction = ref(false);

function minuteToPixel(minute) {
  return (minute - dayStartMinute) * pixelsPerMinute;
}

function pixelToMinute(pixel) {
  return snapToStep(clampMinute(Math.round(pixel / pixelsPerMinute + dayStartMinute)));
}

function clampMinute(minute) {
  return Math.max(dayStartMinute, Math.min(dayEndMinute, minute));
}

function clampEndMinute(minute) {
  return Math.max(dayStartMinute, Math.min(dayEndBoundaryMinute, minute));
}

function parseTime(value) {
  const [hour, minute] = String(value || '00:00').split(':').map(Number);
  return snapToStep(clampMinute(hour * 60 + minute));
}

function snapToStep(minute) {
  return Math.round(minute / stepMinutes) * stepMinutes;
}

function toTime(totalMinutes) {
  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function formatDuration(minutes) {
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

function onTimelineClick(event) {
  if (dragState.value.mode || suppressNextClick.value) {
    suppressNextClick.value = false;
    return;
  }
  hasUserInteraction.value = true;
  showDurationMenu.value = false;
  const rect = event.currentTarget.getBoundingClientRect();
  const y = event.clientY - rect.top + event.currentTarget.scrollTop;
  const newStart = pixelToMinute(y);
  applyStartAndDuration(newStart, props.durationMinutes);
}

function toggleDurationMenu() {
  showDurationMenu.value = !showDurationMenu.value;
}

function selectDuration(duration) {
  hasUserInteraction.value = true;
  showDurationMenu.value = false;
  applyStartAndDuration(startMinutes.value, duration);
}

function startDrag(event) {
  hasUserInteraction.value = true;
  showDurationMenu.value = false;
  const pointerMinute = getPointerMinute(event);
  dragPointerClientY.value = event.clientY;
  dragState.value = {
    mode: 'move',
    baseStart: startMinutes.value,
    baseDuration: props.durationMinutes,
    pointerOffsetMinutes: pointerMinute - startMinutes.value,
    moved: false,
  };
  bindDragListeners();
}

function startResize(edge, event) {
  hasUserInteraction.value = true;
  showDurationMenu.value = false;
  dragPointerClientY.value = event.clientY;
  dragState.value = {
    mode: edge,
    baseStart: startMinutes.value,
    baseDuration: props.durationMinutes,
    pointerOffsetMinutes: 0,
    moved: false,
  };
  bindDragListeners();
}

function onPointerMove(event) {
  dragPointerClientY.value = event.clientY;
  updateDragFromPointer();
}

function updateDragFromPointer() {
  const pointerMinute = getPointerMinuteFromClientY(dragPointerClientY.value);
  dragState.value.moved = true;

  if (dragState.value.mode === 'move') {
    const nextStart = pointerMinute - dragState.value.pointerOffsetMinutes;
    applyStartAndDuration(nextStart, props.durationMinutes);
    return;
  }

  if (dragState.value.mode === 'start') {
    const originalEnd = startMinutes.value + props.durationMinutes;
    const nextStart = clampMinute(pointerMinute);
    const nextDuration = Math.max(minDuration, originalEnd - nextStart);
    applyStartAndDuration(nextStart, nextDuration);
    return;
  }

  if (dragState.value.mode === 'end') {
    const nextDuration = Math.max(minDuration, pointerMinute - startMinutes.value);
    applyStartAndDuration(startMinutes.value, nextDuration);
  }
}

function onPointerUp() {
  if (dragState.value.moved) {
    suppressNextClick.value = true;
  }
  dragPointerClientY.value = null;
  dragState.value = {
    mode: null,
    baseStart: 0,
    baseDuration: 0,
    pointerOffsetMinutes: 0,
    moved: false,
  };
  unbindDragListeners();
}

function bindDragListeners() {
  globalThis.addEventListener('mousemove', onPointerMove);
  globalThis.addEventListener('mouseup', onPointerUp);
  startDragLoop();
}

function unbindDragListeners() {
  globalThis.removeEventListener('mousemove', onPointerMove);
  globalThis.removeEventListener('mouseup', onPointerUp);
  stopDragLoop();
}

function applyStartAndDuration(rawStart, rawDuration) {
  let nextStart = snapToStep(clampMinute(rawStart));
  let nextDuration = snapToStep(Math.max(minDuration, Math.min(maxDuration, rawDuration)));

  const latestEnd = dayEndHour * 60;
  if (nextStart + nextDuration > latestEnd) {
    if (nextDuration > latestEnd - dayStartHour * 60) {
      nextDuration = latestEnd - dayStartHour * 60;
      nextStart = dayStartHour * 60;
    } else {
      nextStart = latestEnd - nextDuration;
    }
  }

  emit('update:startTime', toTime(nextStart));
  emit('update:durationMinutes', nextDuration);
}

function getPointerMinute(event) {
  return getPointerMinuteFromClientY(event.clientY);
}

function getPointerMinuteFromClientY(clientY) {
  if (!scrollHost.value) {
    return startMinutes.value;
  }

  if (clientY == null) {
    return startMinutes.value;
  }

  const rect = scrollHost.value.getBoundingClientRect();
  const y = clientY - rect.top + scrollHost.value.scrollTop;
  return pixelToMinute(y);
}

function maybeAutoScroll(clientY) {
  if (!scrollHost.value) {
    return false;
  }

  if (clientY == null) {
    return false;
  }

  const edgeThreshold = 28;
  const scrollSpeed = 12;
  const rect = scrollHost.value.getBoundingClientRect();
  const previous = scrollHost.value.scrollTop;

  if (clientY < rect.top + edgeThreshold) {
    scrollHost.value.scrollTop = Math.max(0, scrollHost.value.scrollTop - scrollSpeed);
  } else if (clientY > rect.bottom - edgeThreshold) {
    const maxScroll = scrollHost.value.scrollHeight - scrollHost.value.clientHeight;
    scrollHost.value.scrollTop = Math.min(maxScroll, scrollHost.value.scrollTop + scrollSpeed);
  }

  return scrollHost.value.scrollTop !== previous;
}

function startDragLoop() {
  stopDragLoop();

  const tick = () => {
    if (!dragState.value.mode) {
      return;
    }

    const scrolled = maybeAutoScroll(dragPointerClientY.value);
    if (scrolled) {
      updateDragFromPointer();
    }

    dragLoopFrame.value = globalThis.requestAnimationFrame(tick);
  };

  dragLoopFrame.value = globalThis.requestAnimationFrame(tick);
}

function stopDragLoop() {
  if (dragLoopFrame.value) {
    globalThis.cancelAnimationFrame(dragLoopFrame.value);
    dragLoopFrame.value = 0;
  }
}

function scrollSelectionIntoView() {
  if (!scrollHost.value) {
    return;
  }

  const selectionTop = minuteToPixel(startMinutes.value);
  const selectionHeight = Math.max(minDuration * pixelsPerMinute, props.durationMinutes * pixelsPerMinute);
  const selectionCenter = selectionTop + selectionHeight / 2;
  const targetScrollTop = Math.max(0, selectionCenter - scrollHost.value.clientHeight / 2);
  scrollHost.value.scrollTop = targetScrollTop;
}

function onWindowMouseDown(event) {
  if (rootEl.value && !rootEl.value.contains(event.target)) {
    showDurationMenu.value = false;
  }
}

onMounted(() => {
  globalThis.addEventListener('mousedown', onWindowMouseDown);
});

watch(
  [startMinutes, () => props.durationMinutes],
  () => {
    if (dragState.value.mode || hasUserInteraction.value) {
      return;
    }
    nextTick(() => {
      scrollSelectionIntoView();
    });
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  unbindDragListeners();
  stopDragLoop();
  globalThis.removeEventListener('mousedown', onWindowMouseDown);
});
</script>

<style scoped>
.timeline-scroll {
  scrollbar-width: thin;
  scrollbar-color: #7b8bd4 #dce9ff;
}

.timeline-scroll::-webkit-scrollbar {
  width: 10px;
}

.timeline-scroll::-webkit-scrollbar-button {
  display: none;
  width: 0;
  height: 0;
}

.timeline-scroll::-webkit-scrollbar-button:single-button,
.timeline-scroll::-webkit-scrollbar-button:vertical:decrement,
.timeline-scroll::-webkit-scrollbar-button:vertical:increment,
.timeline-scroll::-webkit-scrollbar-button:start,
.timeline-scroll::-webkit-scrollbar-button:end {
  display: none;
  width: 0;
  height: 0;
  background: transparent;
}

.timeline-scroll::-webkit-scrollbar-track {
  background: #dce9ff;
  border-radius: 999px;
}

.timeline-scroll::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #4355b9 0%, #24389c 100%);
  border-radius: 999px;
  border: 2px solid #dce9ff;
}

.timeline-scroll::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #3f51b5 0%, #1f2f85 100%);
}
</style>
