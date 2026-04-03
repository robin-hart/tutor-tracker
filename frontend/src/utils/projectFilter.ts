interface SearchableProject {
  name?: string;
  category?: string;
}

/**
 * Normalizes unknown input for case-insensitive project searching.
 */
export function normalizeForProjectSearch(value: unknown): string {
  return String(value ?? '')
    .toLowerCase()
    .trim();
}

/**
 * Filters projects by name or category using a free-text query.
 */
export function filterProjects<T extends SearchableProject>(projects: T[], query: string): T[] {
  const normalizedQuery = normalizeForProjectSearch(query);
  if (!normalizedQuery) {
    return projects;
  }

  return projects.filter((project) => {
    const name = normalizeForProjectSearch(project?.name);
    const category = normalizeForProjectSearch(project?.category);
    return name.includes(normalizedQuery) || category.includes(normalizedQuery);
  });
}
