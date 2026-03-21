import { chromium } from '@playwright/test';

/**
 * Global setup for E2E tests.
 * 
 * This runs once before all test files and can be used to:
 * - Verify backend API is reachable
 * - Seed test data
 * - Clean up test state
 */
async function globalSetup() {
  const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';
  const frontendBaseUrl = process.env.FRONTEND_BASE_URL || 'http://localhost:5173';

  console.log(`\n📋 Global Setup: Verifying services...`);
  console.log(`   Frontend: ${frontendBaseUrl}`);
  console.log(`   API: ${apiBaseUrl}`);

  // Verify backend API is healthy
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  try {
    const healthResponse = await page.request.get(`${apiBaseUrl}/api/health`);
    if (!healthResponse.ok()) {
      throw new Error(`Backend health check failed: ${healthResponse.status()}`);
    }
    console.log(`   ✅ Backend API is healthy\n`);
  } catch (error) {
    console.error(`   ❌ Backend API check failed: ${error}`);
    throw error;
  } finally {
    await context.close();
    await browser.close();
  }
}

export default globalSetup;
