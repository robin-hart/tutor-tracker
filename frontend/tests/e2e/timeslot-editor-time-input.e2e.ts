import { expect, test, type Page } from '@playwright/test';

const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';

async function createProject(page: Page, suffix: string): Promise<{ id: string }> {
  const response = await page.request.post(`${apiBaseUrl}/api/projects`, {
    data: {
      name: `Timeline ${suffix} ${Date.now()}`,
      institution: 'Timeline Institute',
      category: 'GENERAL',
      targetMonthHours: 12,
      completionPercent: 0,
    },
  });

  expect(response.ok()).toBeTruthy();
  return (await response.json()) as { id: string };
}

test.describe('Timeslot editor timeline input', () => {
  test('creates a slot using timeline click and duration menu', async ({ page }) => {
    const project = await createProject(page, 'Create');
    const date = '2026-04-10';
    const month = '2026-04';
    const title = `Timeline Click ${Date.now()}`;

    await page.goto(`/timeslots/edit?projectId=${project.id}&date=${date}&month=${month}`);
    await expect(page.getByRole('heading', { name: 'Session Log' })).toBeVisible();

    await page.getByLabel('Title').fill(title);
    await page.getByLabel('Description').fill('Created via timeline click and duration menu');

    const timeline = page.getByTestId('timeline-scroll');
    await timeline.click({ position: { x: 90, y: 120 } });

    await page.getByTestId('timeline-duration-edit').click();
    await page.getByTestId('timeline-duration-option-90').click();

    await page.getByRole('button', { name: 'Save Session' }).click();
    await page.waitForURL(`**/projects/${project.id}/calendar**`);

    const response = await page.request.get(
      `${apiBaseUrl}/api/projects/${project.id}/timeslots?month=${month}`
    );
    expect(response.ok()).toBeTruthy();
    const slots = (await response.json()) as Array<{ title: string; durationMinutes: number }>;
    const created = slots.find((slot) => slot.title === title);
    expect(created).toBeTruthy();
    expect(created?.durationMinutes).toBe(90);
  });

  test('updates an existing slot by dragging timeline zone', async ({ page }) => {
    const project = await createProject(page, 'Edit');
    const date = '2026-04-12';
    const month = '2026-04';

    const seed = await page.request.post(`${apiBaseUrl}/api/projects/${project.id}/timeslots`, {
      data: {
        title: `Timeline Drag ${Date.now()}`,
        description: 'Seed for drag/resize test',
        durationMinutes: 60,
        date,
        startTime: '09:00',
      },
    });
    expect(seed.ok()).toBeTruthy();
    const seeded = (await seed.json()) as { id: string };

    await page.goto(
      `/timeslots/edit?projectId=${project.id}&timeslotId=${seeded.id}&date=${date}&month=${month}`
    );

    const zone = page.getByTestId('timeline-selection-zone');
    const zoneBox = await zone.boundingBox();
    expect(zoneBox).toBeTruthy();

    if (!zoneBox) {
      throw new Error('Timeline selection zone not found.');
    }

    await page.mouse.move(zoneBox.x + zoneBox.width / 2, zoneBox.y + zoneBox.height / 2);
    await page.mouse.down();
    await page.mouse.move(zoneBox.x + zoneBox.width / 2, zoneBox.y + zoneBox.height / 2 + 60, {
      steps: 8,
    });
    await page.mouse.up();

    await page.getByRole('button', { name: 'Update Session' }).click();
    await page.waitForURL(`**/projects/${project.id}/calendar**`);

    const response = await page.request.get(
      `${apiBaseUrl}/api/projects/${project.id}/timeslots?month=${month}`
    );
    expect(response.ok()).toBeTruthy();
    const slots = (await response.json()) as Array<{ id: string; startTime: string }>;
    const updated = slots.find((slot) => slot.id === seeded.id);

    expect(updated).toBeTruthy();
    expect(updated?.startTime).not.toBe('09:00');
  });
});
