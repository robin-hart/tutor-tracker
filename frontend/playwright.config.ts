import { defineConfig, devices } from '@playwright/test';

/**
 * Read environment variables for test configuration.
 * API_BASE_URL: Tutor Time Tracker backend API (default: http://localhost:8080)
 * FRONTEND_BASE_URL: Frontend app URL (default: http://localhost:5173)
 * HEADLESS: Run tests in headless mode (default: true)
 */
const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';
const frontendBaseUrl = process.env.FRONTEND_BASE_URL || 'http://localhost:5173';
const headless = process.env.HEADLESS !== 'false';

// UI mode only works with single browser
const isUIMode = process.argv.includes('--ui');

export default defineConfig({
  testDir: './tests/e2e',
  testMatch: '**/*.e2e.ts',
  fullyParallel: !isUIMode,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: isUIMode ? 1 : process.env.CI ? 1 : undefined,

  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: 'html',

  /* Shared settings for all named projects. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: frontendBaseUrl,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  /* Configure projects: Chromium only */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], headless: isUIMode ? false : headless },
    },
  ],

  /* Run your local dev server before starting the tests */
  webServer: [
    {
      command: 'npm run dev',
      url: frontendBaseUrl,
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
    },
    {
      command: 'cd ../api && mvn spring-boot:run -Dspring-boot.run.profiles=test',
      url: `${apiBaseUrl}/api/health`,
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
    },
  ],
});
