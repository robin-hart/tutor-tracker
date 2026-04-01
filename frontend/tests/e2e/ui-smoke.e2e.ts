import { expect, test, type APIRequestContext } from '@playwright/test';

const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';

/**
 * Creates a project for smoke tests using the public API.
 */
async function createSmokeProject(
  request: APIRequestContext,
  suffix: string
): Promise<{ id: string; name: string }> {
  const projectName = `Smoke ${suffix} ${Date.now()}`;
  const response = await request.post(`${apiBaseUrl}/api/projects`, {
    data: {
      name: projectName,
      category: 'GENERAL',
      totalHours: 0,
      monthHours: 0,
      completionPercent: 0,
    },
  });

  expect(response.ok()).toBeTruthy();
  const project = (await response.json()) as { id: string };
  return { id: project.id, name: projectName };
}

/**
 * Multi-flow smoke tests focused on critical UI paths.
 */
test.describe('UI smoke journeys', () => {
  test('loads dashboard and creates a project from UI', async ({ page }) => {
    await page.goto('/projects');
    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();

    const projectName = `PW UI Smoke ${Date.now()}`;
    await page.getByRole('button', { name: /new project/i }).click();
    await page.getByPlaceholder('Project name').fill(projectName);
    await page
      .getByRole('button', { name: /create/i })
      .last()
      .click();

    await expect(page.getByText(projectName)).toBeVisible();
  });

  test('opens calendar view for a project', async ({ page }) => {
    const project = await createSmokeProject(page.request, 'Calendar');

    await page.goto(`/projects/${project.id}/calendar`);

    await expect(page.getByRole('heading', { name: project.name })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Prev' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Next' })).toBeVisible();
  });

  test('adds a student from student management view', async ({ page }) => {
    const project = await createSmokeProject(page.request, 'Students');
    const studentName = `Smoke Student ${Date.now()}`;

    await page.goto(`/projects/${project.id}/students`);

    await expect(page.getByRole('heading', { name: 'Manage Students' })).toBeVisible();
    await page.getByRole('button', { name: 'Add New Student' }).click();
    await page.getByPlaceholder('Name').fill(studentName);
    await page.getByRole('button', { name: 'Add', exact: true }).first().click();

    await expect(page.getByText(studentName)).toBeVisible();
  });

  test('saves a timeslot from timeslot editor', async ({ page }) => {
    const project = await createSmokeProject(page.request, 'Timeslot');
    const date = new Date().toISOString().slice(0, 10);
    const month = date.slice(0, 7);
    const title = `Smoke Session ${Date.now()}`;

    await page.goto(`/timeslots/edit?projectId=${project.id}&date=${date}&month=${month}`);

    await expect(page.getByRole('heading', { name: 'Session Log' })).toBeVisible();
    await page.getByLabel('Title').fill(title);
    await page.getByLabel('Description').fill('Smoke test timeslot creation');
    await page.locator('input[type="number"]').fill('60');
    await page.locator('input[type="time"]').fill('14:00');

    await page.getByRole('button', { name: 'Save Session' }).click();
    await page.waitForURL(`**/projects/${project.id}/calendar**`);

    const response = await page.request.get(
      `${apiBaseUrl}/api/projects/${project.id}/timeslots?month=${month}`
    );
    expect(response.ok()).toBeTruthy();
    const slots = await response.json();
    expect(slots.some((slot: { title: string }) => slot.title === title)).toBeTruthy();
  });

  test('generates monthly report from reports view', async ({ page }) => {
    const project = await createSmokeProject(page.request, 'Reports');
    const today = new Date();
    const month = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;

    const slotResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${project.id}/timeslots`,
      {
        data: {
          title: `Report seed ${Date.now()}`,
          description: 'Smoke report seed data',
          durationMinutes: 60,
          date: `${month}-15`,
          startTime: '10:00',
        },
      }
    );
    expect(slotResponse.ok()).toBeTruthy();

    await page.goto('/reports');

    await expect(page.getByRole('heading', { name: 'Reports & Exports' })).toBeVisible();
    await page.locator('select').first().selectOption(project.id);

    // Wait for the month list to load in the table
    await page.locator('table tbody tr').first().waitFor({ state: 'visible' });

    const exportResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/api/projects/${project.id}/reports/export/pdf`) &&
        response.request().method() === 'GET'
    );

    await page.getByRole('button', { name: 'Generate Newest Monthly Report' }).click();
    const exportResponse = await exportResponsePromise;
    expect(exportResponse.ok()).toBeTruthy();
  });
});


