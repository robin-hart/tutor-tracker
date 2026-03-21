# End-to-End Testing with Playwright

Automated end-to-end (E2E) testing using **Playwright** for testing user workflows across the frontend and backend.

## Overview

**Playwright** is a modern, fast, and reliable framework for automating browser interactions. It supports multiple browsers (Chromium, Firefox, WebKit) and is perfect for testing real user workflows.

### Why Playwright?

- ✅ **Multi-browser support** - Test across Chromium, Firefox, and WebKit on the same code
- ✅ **Fast & reliable** - Automatic waiting, intelligent locators, and built-in retry logic
- ✅ **API testing included** - Full control over HTTP requests without leaving the test context
- ✅ **Great debugging** - UI mode, trace viewer, and detailed error reports
- ✅ **CI/CD ready** - Built-in support for parallel execution and reporting
- ✅ **TypeScript support** - Full type safety for test code

## Test Data & Database Setup

### Important: Test Data Creation

Tests **do NOT use pre-populated demo data**. Instead:

1. **Each test creates its own data via API calls** in `beforeAll` and `beforeEach` hooks
2. Tests use the **default MariaDB** (not a separate test database)
3. **Tests are isolated** - each creates test projects/students/timeslots with unique timestamps
4. **No data cleanup** - tests leave their created data in the database (safe for stale test data accumulation over time)

### Backend Database

- **Default profile**: Uses MariaDB with demo data for manual testing
- **Test profile**: Uses H2 in-memory database (for unit & integration tests in `api/`)
- **E2E tests**: Run against the **default profile** with MariaDB (tests the real database setup)

Since tests create unique data (using timestamps), they won't conflict with demo data or other test runs.

### Example: How Tests Create Data

```typescript
test.beforeAll(async ({ playwright }) => {
  // Create a test project with unique slug
  const projectResponse = await page.request.post(`${apiBaseUrl}/api/projects`, {
    data: {
      name: `Test Project ${Date.now()}`,
      slug: `test-${Date.now()}`,
    },
  });
  
  const project = await projectResponse.json();
  testProjectSlug = project.slug; // Use this in tests
});

test('should create student in project', async () => {
  // Test data is created via API
  const studentResponse = await page.request.post(
    `${apiBaseUrl}/api/projects/${testProjectSlug}/students`,
    { data: { name: 'Test Student', notes: '', groupName: 'Group A' } }
  );
  
  expect(studentResponse.ok()).toBeTruthy();
});
```

## Project Structure

```
frontend/
├── tests/
│   ├── e2e/
│   │   ├── global-setup.ts           # Health checks; verifies API is reachable
│   │   ├── projects.e2e.ts           # Project management workflows
│   │   ├── students.e2e.ts           # Student management workflows
│   │   ├── timeslots.e2e.ts          # Calendar & timeslot workflows
│   │   └── reports.e2e.ts            # Report generation workflows
│   └── ...                            # Unit tests (vitest)
├── playwright.config.ts               # Playwright configuration
└── package.json
```

## Running Tests

### Run all E2E tests (headless, all 3 browsers)
```bash
npm run e2e
```

### Run tests in UI mode (interactive, single browser)
**This is the recommended way to develop and debug tests:**
```bash
npm run e2e:ui
```

Opens an interactive UI where you can:
- ▶️ Run individual tests
- 📹 Watch live browser interactions
- 🐛 Debug failing tests
- 📊 View test traces and screenshots
- ⏸️ Pause and step through code

UI mode uses **Chromium in headed mode** (browser window visible) for a Better development experience.

### Run specific test file
```bash
npx playwright test tests/e2e/projects.e2e.ts
```

### Run tests matching a pattern
```bash
npx playwright test -g "should create"
```

### Debug mode (step-through with Inspector)
```bash
npm run e2e:debug
```
Opens the Playwright Inspector, allowing you to step through each action and inspect the DOM.

### View test report
```bash
npm run e2e:report
```
Opens the HTML test report in your browser showing results, traces, and failure details.

## Configuration

Edit `playwright.config.ts` to customize test behavior:

### Environment Variables

- **API_BASE_URL** - Backend API URL (default: `http://localhost:8080`)
- **FRONTEND_BASE_URL** - Frontend app URL (default: `http://localhost:5173`)
- **HEADLESS** - Run tests in headless mode (default: `true`)

### Example: Test Against Staging

```bash
API_BASE_URL=https://api.staging.example.com \
FRONTEND_BASE_URL=https://staging.example.com \
npm run e2e
```

### Example: Run with visible browser

```bash
HEADLESS=false npm run e2e
```

## Test Patterns

### Using API Requests in E2E Tests

You can make HTTP requests directly from tests without opening the browser:

```typescript
const response = await page.request.get(`${apiBaseUrl}/api/projects`);
expect(response.ok()).toBeTruthy();
const data = await response.json();
```

### Creating Test Data

Use API calls in `beforeAll` hooks to set up test projects/students/timeslots:

```typescript
test.beforeAll(async ({ playwright }) => {
  const browser = await playwright.chromium.launch();
  const context = await browser.createBrowserContext();
  const page = await context.newPage();

  // Create test project
  const projectResponse = await page.request.post(
    `${apiBaseUrl}/api/projects`,
    { data: { name: `Test Project ${Date.now()}`, slug: `test-${Date.now()}` } }
  );

  const project = await projectResponse.json();
  testProjectSlug = project.slug;

  await context.close();
  await browser.close();
});
```

### Assertions

Common assertion patterns:

```typescript
// HTTP response checks
expect(response.ok()).toBeTruthy();
expect(response.status()).toBe(200);

// JSON response validation
const data = await response.json();
expect(data).toHaveProperty('id');
expect(data.name).toBe('Expected Name');

// Array validation
expect(Array.isArray(data)).toBeTruthy();
```

## Workflow: Local Development

1. **Start backend** (Terminal 1)
   ```bash
   cd api && mvn spring-boot:run
   ```

2. **Start frontend** (Terminal 2)
   ```bash
   npm run dev
   ```

3. **Run E2E tests in UI mode** (Terminal 3)
   ```bash
   npm run e2e:ui
   ```

The tests will automatically wait for both services to be ready before running.

**Note:** Playwright also auto-starts services, so if you don't manually start them, it will do so automatically.

## Workflow: Before Committing

```bash
# Run unit tests
npm run test:run

# Run E2E tests
npm run e2e

# If either fails, fix and re-run
```

## Workflow: CI/CD Integration

In GitHub Actions / GitLab CI / Jenkins:

```yaml
- name: Start Backend
  run: cd api && mvn spring-boot:run &
  
- name: Run E2E Tests
  run: npm run e2e
  env:
    API_BASE_URL: http://localhost:8080
    FRONTEND_BASE_URL: http://localhost:5173
    CI: true  # Single worker, 2 retries, parallel disabled
```

## Debugging Failed Tests

### View detailed trace
```bash
npx playwright show-trace trace.zip
```

### See screenshots
Failed tests automatically capture screenshots (saved in `test-results/` by default).

### Use the Inspector
```bash
npm run e2e:debug
```
Then use the Playwright Inspector console to evaluate expressions and step through code.

### Add debug statements
```typescript
await page.pause();  // Pauses execution, opens browser for inspection
console.log(await page.textContent('body'));  // Log page content
```

### View recent failures
```bash
npm run e2e:report
```
Shows the last test run with all failures, traces, and screenshots.

## Best Practices

1. **Keep tests isolated** - Each test should be independent and not rely on test execution order
2. **Use API calls for setup** - Create test data via API in `beforeAll`/`beforeEach` rather than UI clicks
3. **Prefer API assertions** - Test API contracts directly, not just UI rendering
4. **Use meaningful test names** - Test names should describe the user workflow
5. **Avoid hard waits** - Use Playwright's auto-waiting instead of `sleep()`
6. **Test real workflows** - Focus on critical user journeys, not implementation details
7. **Use unique data** - Create test data with timestamps to avoid conflicts

## Adding New Tests

1. Create a new file: `tests/e2e/feature.e2e.ts`
2. Import Playwright test utilities
3. Define test suite with `test.describe()`
4. Write test cases with `test()`
5. Use `beforeAll` / `beforeEach` for setup
6. Make API calls and assertions

Template:

```typescript
import { test, expect, Page } from '@playwright/test';

test.describe('Feature Name', () => {
  let page: Page;
  let testProjectSlug: string;
  const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';

  test.beforeAll(async ({ playwright }) => {
    // Create test project via API
    const browser = await playwright.chromium.launch();
    const context = await browser.createBrowserContext();
    const setupPage = await context.newPage();

    const response = await setupPage.request.post(`${apiBaseUrl}/api/projects`, {
      data: {
        name: `Test Project ${Date.now()}`,
        slug: `test-${Date.now()}`,
      },
    });

    const project = await response.json();
    testProjectSlug = project.slug;

    await context.close();
    await browser.close();
  });

  test.beforeEach(async ({ page: testPage }) => {
    page = testPage;
  });

  test('should do something', async () => {
    // Arrange: Set up data (already done in beforeAll)
    // Act: Perform action via API
    const response = await page.request.post(`${apiBaseUrl}/api/...`, { data: {...} });
    
    // Assert: Verify result
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.id).toBeTruthy();
  });
});
```

## Troubleshooting

### Tests timeout
- Increase timeout in `playwright.config.ts`
- Verify backend is running: `curl http://localhost:8080/api/health`
- Verify frontend is running: `curl http://localhost:5173`
- Check network connectivity

### "Browser not found"
```bash
npx playwright install
```

### Port already in use
- Check what's using ports 5173 and 8080
- Kill existing processes or use different ports:
  ```bash
  FRONTEND_BASE_URL=http://localhost:5174 npm run e2e
  ```

### UI mode doesn't open browser
- Ensure you're running with `npm run e2e:ui` (not raw `playwright test --ui`)
- UI mode only works with Chromium in headed mode
- If it still doesn't work, use `npm run e2e:debug` instead

### Flaky tests
- Remove hard waits (use auto-waiting instead)
- Add retries for API calls
- Use `waitForLoadState('networkidle')`
- Use unique test data (with timestamps)

### Tests interfering with each other
- Each test creates unique data with timestamps
- If tests are deleting shared demo data, that's a test isolation issue
- Review `beforeAll` and `beforeEach` hooks to ensure proper setup

## Resources

- **Playwright Docs**: https://playwright.dev
- **Test Examples**: https://github.com/microsoft/playwright/tree/main/examples
- **API Testing**: https://playwright.dev/docs/api-testing
- **Debugging**: https://playwright.dev/docs/debug
- **Best Practices**: https://playwright.dev/docs/best-practices

## Questions?

Refer to the test files in `tests/e2e/` for working examples of all major workflows.
