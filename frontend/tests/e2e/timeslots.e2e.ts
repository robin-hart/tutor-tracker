import { test, expect, Page } from '@playwright/test';

/**
 * E2E tests for Calendar and Timeslot management
 *
 * Tests user flows:
 * - Creating timeslots in project calendar
 * - Viewing calendar for a month
 * - Updating timeslots
 * - Deleting timeslots
 */

test.describe('Calendar & Timeslots', () => {
  let page: Page;
  let testProjectId: string;
  const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';

  test.beforeAll(async ({ playwright }) => {
    // Create a test project for timeslot tests
    const browser = await playwright.chromium.launch();
    const context = await browser.newContext();
    const setupPage = await context.newPage();

    const projectResponse = await setupPage.request.post(`${apiBaseUrl}/api/projects`, {
      data: {
        name: `Timeslot Test Project ${Date.now()}`,
        institution: 'Timeslot Institute',
        category: 'GENERAL',
        targetMonthHours: 10,
        completionPercent: 0,
      },
    });

    if (!projectResponse.ok()) {
      throw new Error(
        `Failed to create test project: ${projectResponse.status()} ${projectResponse.statusText()}`
      );
    }
    const project = await projectResponse.json();
    testProjectId = project.id;

    await context.close();
    await browser.close();
  });

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
  });

  test('should create a timeslot via API', async () => {
    const timeslotData = {
      title: `Automated Test Session ${Date.now()}`,
      description: 'E2E test timeslot',
      durationMinutes: 60,
      date: '2026-03-25',
      startTime: '14:00',
    };

    const createResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots`,
      {
        data: timeslotData,
      }
    );

    expect(createResponse.ok()).toBeTruthy();
    const createdTimeslot = await createResponse.json();
    expect(createdTimeslot.title).toBe(timeslotData.title);
    expect(createdTimeslot.id).toBeTruthy();
  });

  test('should retrieve calendar for project and month', async () => {
    const calendarResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/calendar?month=2026-03`
    );

    expect(calendarResponse.ok()).toBeTruthy();
    const calendar = await calendarResponse.json();

    expect(calendar).toHaveProperty('monthSlots');
    expect(Array.isArray(calendar.monthSlots)).toBeTruthy();
  });

  test('should get timeslots for a specific month', async () => {
    const timeslotsResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots?month=2026-03`
    );

    expect(timeslotsResponse.ok()).toBeTruthy();
    const timeslots = await timeslotsResponse.json();
    expect(Array.isArray(timeslots)).toBeTruthy();
  });

  test('should update a timeslot via API', async () => {
    // Create a timeslot
    const createResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots`,
      {
        data: {
          title: `Session to Update ${Date.now()}`,
          description: 'Original description',
          durationMinutes: 60,
          date: '2026-03-26',
          startTime: '10:00',
        },
      }
    );

    const timeslot = await createResponse.json();

    // Update the timeslot
    const updatedData = {
      title: 'Updated Session Title',
      description: 'Updated description',
      durationMinutes: 90,
      date: '2026-03-26',
      startTime: '11:00',
    };

    const updateResponse = await page.request.put(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots/${timeslot.id}`,
      {
        data: updatedData,
      }
    );

    expect(updateResponse.ok()).toBeTruthy();
    const updatedTimeslot = await updateResponse.json();
    expect(updatedTimeslot.title).toBe(updatedData.title);
    expect(updatedTimeslot.durationMinutes).toBe(updatedData.durationMinutes);
  });

  test('should delete a timeslot via API', async () => {
    // Create a timeslot
    const createResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots`,
      {
        data: {
          title: `Session to Delete ${Date.now()}`,
          description: 'Will be deleted',
          durationMinutes: 45,
          date: '2026-03-27',
          startTime: '15:00',
        },
      }
    );

    const timeslot = await createResponse.json();

    // Delete the timeslot
    const deleteResponse = await page.request.delete(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots/${timeslot.id}`
    );

    expect(deleteResponse.status()).toBe(204); // No Content
  });

  test('should verify timeslot API response structure', async () => {
    const timeslotsResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/timeslots`
    );

    const timeslots = await timeslotsResponse.json();

    if (timeslots.length > 0) {
      const timeslot = timeslots[0];

      // Verify required fields
      expect(timeslot).toHaveProperty('id');
      expect(timeslot).toHaveProperty('title');
      expect(timeslot).toHaveProperty('date');
      expect(timeslot).toHaveProperty('startTime');
    }
  });
});
