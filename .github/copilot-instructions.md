# Copilot Instructions for Tutor Time Tracker

## Branching model

- `main` is the production branch.
- `develop` is the integration branch for ongoing development.
- Feature branches are created from `develop` and merged back when complete. Feature branch naming convention: `feature_<short-description>`.

## Code style

- Java: Follow Google Java Style Guide. Use Checkstyle enforcement.
- JavaScript/TypeScript: Use ESLint enforcement and Prettier for formatting. Continuously change the prettier file to match the style of the existing codebase (carefully).

Continuously run linting and formatting checks during development.

## Testing

- Backend: Use JUnit 5 for unit tests and Spring Boot Test for integration tests. Aim for a very high line and branch coverage. Use Mockito for mocking dependencies in unit tests. Use an in-memory H2 database for integration tests to ensure isolation and repeatability.
Provide good names and clear assertions in tests to ensure they are human-readable and maintainable. For the purpose of own validation, also use the jacoco plugin and reports.

- Frontend: Use Vitest for unit tests and Playwright for end-to-end tests. Aim for very high coverage of components. Build real smoke tests for every normal case. Those test should be human readable and will be validated by humans.

## Documentation

- Maintain up-to-date Javadoc comments for all public classes and methods in the backend. Do not add Javadoc to private methods or fields unless it adds significant clarity. Do not add unnecessary comments, that just explain how code works.
- Maintain clear JSDoc comments for frontend modules and components.
- Update the architecture documentation in `ARCHITECTURE.md` as design decisions evolve.
- Keep the README files in sync with the actual project structure and setup instructions.
- Ensure API endpoint contracts are clearly documented in both the architecture docs and the backend code.