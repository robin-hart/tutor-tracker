import { expect, test } from '@playwright/test';

/**
 * UI smoke flow meant for Playwright UI mode.
 * This test intentionally performs visible user actions so the browser is not stuck on about:blank.
 */
test('user can open dashboard and create a project from UI', async ({ page }) => {
  await page.goto('/projects');

  await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();

  const newProjectButton = page.getByRole('button', { name: /new project/i });
  await newProjectButton.click();

  const projectName = `PW UI Smoke ${Date.now()}`;
  await page.getByPlaceholder('Project name').fill(projectName);

  await page
    .getByRole('button', { name: /create/i })
    .last()
    .click();

  await expect(page.getByText(projectName)).toBeVisible();
});
