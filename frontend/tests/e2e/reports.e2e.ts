import { test, expect, Page } from '@playwright/test';

/**
 * E2E tests for Reports functionality
 *
 * Tests user flows:
 * - Generating reports from timeslots
 * - Retrieving project reports
 * - Verifying report data
 */

test.describe('Reports', () => {
  let page: Page;
  let testProjectId: string;
  const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';

  test.beforeAll(async ({ playwright }) => {
    // Create a test project with timeslots for report generation
    const browser = await playwright.chromium.launch();
    const context = await browser.newContext();
    const setupPage = await context.newPage();

    // Create project
    const projectResponse = await setupPage.request.post(`${apiBaseUrl}/api/projects`, {
      data: {
        name: `Report Test Project ${Date.now()}`,
        category: 'GENERAL',
        totalHours: 0,
        monthHours: 0,
        completionPercent: 0,
      },
    });

    expect(projectResponse.ok()).toBeTruthy();
    const project = await projectResponse.json();
    testProjectId = project.id;

    // Create some timeslots for the current month
    const today = new Date();
    const currentMonth = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;

    for (let i = 1; i <= 3; i++) {
      await setupPage.request.post(`${apiBaseUrl}/api/projects/${testProjectId}/timeslots`, {
        data: {
          title: `Sample Lesson ${i}`,
          description: `Lesson on topic ${i}`,
          durationMinutes: 60,
          date: `${currentMonth}-${String(10 + i).padStart(2, '0')}`,
          startTime: '14:00',
        },
      });
    }

    await context.close();
    await browser.close();
  });

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
  });

  test('should generate report for current month', async () => {
    const today = new Date();
    const currentMonth = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
    const currentYear = `${today.getFullYear()}`;

    const generateResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/reports/generate?month=${currentMonth}`,
      {
        data: {},
      }
    );

    expect(generateResponse.ok()).toBeTruthy();
    const report = await generateResponse.json();
    expect(report).toHaveProperty('projectId');
    expect(report).toHaveProperty('projectName');
    expect(report).toHaveProperty('month');
    expect(report).toHaveProperty('sessions');
    expect(report).toHaveProperty('totalHours');
    expect(report.projectId).toBe(testProjectId);
    expect(typeof report.month).toBe('string');
    expect(report.month).toContain(currentYear);
    expect(currentMonth).toContain(currentYear);
  });

  test('should retrieve reports for project', async () => {
    const reportsResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/reports`
    );

    expect(reportsResponse.ok()).toBeTruthy();
    const reports = await reportsResponse.json();
    expect(Array.isArray(reports)).toBeTruthy();
  });

  test('should retrieve all reports', async () => {
    const reportsResponse = await page.request.get(`${apiBaseUrl}/api/reports`);

    expect(reportsResponse.ok()).toBeTruthy();
    const reports = await reportsResponse.json();
    expect(Array.isArray(reports)).toBeTruthy();
  });

  test('should verify report structure', async () => {
    const reportsResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/reports`
    );

    expect(reportsResponse.ok()).toBeTruthy();

    const reports = await reportsResponse.json();
    expect(Array.isArray(reports)).toBeTruthy();

    if (reports.length > 0) {
      const report = reports[0];

      // Verify expected fields
      expect(report).toHaveProperty('projectId');
      expect(report).toHaveProperty('projectName');
      expect(report).toHaveProperty('month');
      expect(report).toHaveProperty('sessions');
      expect(report).toHaveProperty('totalHours');
      expect(report).toHaveProperty('grossAmount');

      // Verify month format
      const monthRegex = /^[A-Za-z]+\s\d{4}$/;
      expect(report.month).toMatch(monthRegex);
      expect(report.projectId).toBe(testProjectId);
    }
  });
});
