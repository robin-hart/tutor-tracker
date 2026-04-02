import { test, expect, type Page } from '@playwright/test';

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
        institution: 'Report Institute',
        category: 'GENERAL',
        targetMonthHours: 10,
        completionPercent: 0,
      },
    });

    expect(projectResponse.ok()).toBeTruthy();
    const project = (await projectResponse.json()) as { id: string };
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

  test('should display project name for months with zero timeslots', async () => {
    // Create a project with timeslots only in January 2026
    const browser = page.context().browser();
    expect(browser).not.toBeNull();
    if (!browser) {
      throw new Error('Browser instance is not available in Playwright context.');
    }
    const tempContext = await browser.newContext();
    const tempPage = await tempContext.newPage();

    const projectResponse = await tempPage.request.post(`${apiBaseUrl}/api/projects`, {
      data: {
        name: `Zero Hours Test Project ${Date.now()}`,
        institution: 'Test Institute',
        category: 'TEST',
        targetMonthHours: 8,
        completionPercent: 0,
      },
    });

    expect(projectResponse.ok()).toBeTruthy();
    const project = (await projectResponse.json()) as { id: string };
    const projectId = project.id;
    const projName = 'Zero Hours Test Project';

    // Create timeslots only in January 2026
    const january2026 = '2026-01';
    for (let i = 1; i <= 2; i++) {
      const slotResponse = await tempPage.request.post(
        `${apiBaseUrl}/api/projects/${projectId}/timeslots`,
        {
          data: {
            title: `January Lesson ${i}`,
            description: `January timeslot ${i}`,
            durationMinutes: 60,
            date: `${january2026}-${String(10 + i).padStart(2, '0')}`,
            startTime: '10:00',
          },
        }
      );
      expect(slotResponse.ok()).toBeTruthy();
    }

    await tempContext.close();

    // Now navigate to the export reports page and verify the display
    await page.goto('/reports');
    await expect(page.getByRole('heading', { name: 'Reports & Exports' })).toBeVisible();

    const projectSelect = page.locator('select').first();
    const projectOption = page.locator(`select option[value="${projectId}"]`);
    await expect(projectOption).toHaveCount(1);
    await projectSelect.selectOption({ value: projectId });

    // Wait for the month list and refreshed project data
    await page.locator('table tbody tr').first().waitFor({ state: 'visible' });
    await expect(page.locator('table tbody tr').first().locator('td').nth(1)).toContainText(projName);

    // Get all rows in the table
    const rows = await page.locator('table tbody tr').all();

    // Should have rows for January through April 2026 (based on current date 2026-04-01)
    expect(rows.length).toBeGreaterThanOrEqual(4);

    // Check that all months have the project name displayed
    for (const row of rows) {
      const cells = await row.locator('td').all();
      // Second column should be project name
      if (cells.length >= 2) {
        const projectNameCell = await cells[1].textContent();
        expect(projectNameCell).toContain(projName);
        // Should NOT display the fallback '—' symbol
        expect(projectNameCell).not.toBe('—');
      }
    }
  });

  test('should show institution, target hours, formatted totals and transfer state in month list', async () => {
    const browser = page.context().browser();
    expect(browser).not.toBeNull();
    if (!browser) {
      throw new Error('Browser instance is not available in Playwright context.');
    }

    const tempContext = await browser.newContext();
    const tempPage = await tempContext.newPage();

    const projectResponse = await tempPage.request.post(`${apiBaseUrl}/api/projects`, {
      data: {
        name: `Carryover Smoke Project ${Date.now()}`,
        institution: 'Carryover Institute',
        category: 'GENERAL',
        targetMonthHours: 10,
        completionPercent: 0,
      },
    });

    expect(projectResponse.ok()).toBeTruthy();
    const project = (await projectResponse.json()) as { id: string; name: string };

    const slotPayloads = [
      { title: 'Jan 1', date: '2026-01-06', durationMinutes: 120 },
      { title: 'Jan 2', date: '2026-01-20', durationMinutes: 120 },
      { title: 'Mar 1', date: '2026-03-12', durationMinutes: 180 },
      { title: 'Apr 1', date: '2026-04-03', durationMinutes: 432 },
      { title: 'Apr 2', date: '2026-04-09', durationMinutes: 432 },
      { title: 'Apr 3', date: '2026-04-15', durationMinutes: 432 },
      { title: 'Apr 4', date: '2026-04-21', durationMinutes: 432 },
      { title: 'Apr 5', date: '2026-04-27', durationMinutes: 432 },
    ];

    for (const slot of slotPayloads) {
      const slotResponse = await tempPage.request.post(`${apiBaseUrl}/api/projects/${project.id}/timeslots`, {
        data: {
          title: slot.title,
          description: `Smoke slot ${slot.title}`,
          durationMinutes: slot.durationMinutes,
          date: slot.date,
          startTime: '09:00',
        },
      });
      expect(slotResponse.ok()).toBeTruthy();
    }

    await tempContext.close();

    await page.goto('/reports');
    await expect(page.getByRole('heading', { name: 'Reports & Exports' })).toBeVisible();

    await page.getByTestId('report-project-select').selectOption({ value: project.id });
    await page.locator('table tbody tr').first().waitFor({ state: 'visible' });

    await expect(page.getByTestId('report-project-institution')).toContainText('Carryover Institute');
    await expect(page.getByTestId('report-project-target')).toContainText('10h 00min');

    const firstTotalHours = page.getByTestId('month-total-hours').first();
    await expect(firstTotalHours).toContainText('h');
    await expect(firstTotalHours).toContainText('min');
    await expect(firstTotalHours).not.toContainText('hrs');

    const firstTransfer = page.getByTestId('month-transfer-next').first();
    await expect(firstTransfer).toContainText('3h 00min');

    const januaryRow = page.locator('table tbody tr').filter({ hasText: 'Januar 2026' }).first();
    await expect(januaryRow).toBeVisible();
    await expect(januaryRow.getByTestId('month-transfer-next')).toContainText('-6h 00min');
    await expect(page.locator('table tbody tr').filter({ hasText: project.name }).first()).toBeVisible();
  });
});


