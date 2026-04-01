import { test, expect, type Page } from '@playwright/test';

/**
 * E2E tests for Student management within projects
 *
 * Tests user flows:
 * - Creating a student in a project
 * - Viewing students list
 * - Updating student notes
 * - Assigning students to groups
 */

test.describe('Student Management', () => {
  let page: Page;
  let testProjectId: string;
  const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';

  test.beforeAll(async ({ playwright }) => {
    // Create a test project for student tests
    const browser = await playwright.chromium.launch();
    const context = await browser.newContext();
    const setupPage = await context.newPage();

    const projectResponse = await setupPage.request.post(`${apiBaseUrl}/api/projects`, {
      data: {
        name: `Student Test Project ${Date.now()}`,
        category: 'GENERAL',
        totalHours: 0,
        monthHours: 0,
        completionPercent: 0,
      },
    });

    expect(projectResponse.ok()).toBeTruthy();
    const project = (await projectResponse.json()) as { id: string };
    testProjectId = project.id;

    // Seed explicit groups used in test cases.
    await setupPage.request.post(`${apiBaseUrl}/api/projects/${testProjectId}/groups`, {
      data: { name: 'Group A' },
    });
    await setupPage.request.post(`${apiBaseUrl}/api/projects/${testProjectId}/groups`, {
      data: { name: 'Group B' },
    });

    await context.close();
    await browser.close();
  });

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
  });

  test('should create a new student via API', async () => {
    const studentName = `Test Student ${Date.now()}`;

    const createResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/students`,
      {
        data: {
          name: studentName,
          notes: 'Initial test notes',
          groupName: 'Group A',
        },
      }
    );

    expect(createResponse.ok()).toBeTruthy();
    const createdStudent = (await createResponse.json()) as { id: string; name: string };
    expect(createdStudent.name).toBe(studentName);
    expect(createdStudent.id).toBeTruthy();
  });

  test('should retrieve students list for project', async () => {
    const studentsResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/students`
    );

    expect(studentsResponse.ok()).toBeTruthy();
    const students = await studentsResponse.json();
    expect(Array.isArray(students)).toBeTruthy();
  });

  test('should update student notes via API', async () => {
    // Create a student first
    const createResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/students`,
      {
        data: {
          name: `Student for notes update ${Date.now()}`,
          notes: 'Original notes',
          groupName: 'Group A',
        },
      }
    );

    const student = (await createResponse.json()) as { id: string };
    const studentId = student.id;

    // Update notes
    const newNotes = 'Updated notes after UI interaction';
    const updateResponse = await page.request.patch(
      `${apiBaseUrl}/api/students/${studentId}/notes`,
      {
        data: {
          notes: newNotes,
        },
      }
    );

    expect(updateResponse.ok()).toBeTruthy();
    const updatedStudent = (await updateResponse.json()) as { notes: string };
    expect(updatedStudent.notes).toBe(newNotes);
  });

  test('should reassign student to different group', async () => {
    // Create a student
    const createResponse = await page.request.post(
      `${apiBaseUrl}/api/projects/${testProjectId}/students`,
      {
        data: {
          name: `Student for group assignment ${Date.now()}`,
          notes: 'Test group assignment',
          groupName: 'Group A',
        },
      }
    );

    const student = (await createResponse.json()) as { id: string };
    const studentId = student.id;

    // Reassign to Group B
    const reassignResponse = await page.request.patch(
      `${apiBaseUrl}/api/students/${studentId}/group`,
      {
        data: {
          groupName: 'Group B',
        },
      }
    );

    expect(reassignResponse.ok()).toBeTruthy();
    const updatedStudent = (await reassignResponse.json()) as { groupName: string };
    expect(updatedStudent.groupName).toBe('Group B');
  });

  test('should verify student API response structure', async () => {
    const studentsResponse = await page.request.get(
      `${apiBaseUrl}/api/projects/${testProjectId}/students`
    );

    const students = await studentsResponse.json();

    if (students.length > 0) {
      const student = students[0];

      // Verify required fields
      expect(student).toHaveProperty('id');
      expect(student).toHaveProperty('name');
      expect(student).toHaveProperty('notes');
      expect(student).toHaveProperty('groupName');

      expect(typeof student.id).toBe('string');
      expect(typeof student.name).toBe('string');
    }
  });
});
