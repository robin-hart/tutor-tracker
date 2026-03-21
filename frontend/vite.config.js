import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

/**
 * Vite configuration for TutorTimeTracker frontend.
 *
 * @returns {import('vite').UserConfigExport}
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true
  }
});
