/**
 * Normalizes values for case-insensitive project filtering.
 *
 * @param {unknown} value
 * @returns {string}
 */
export function normalizeForProjectSearch(value) {
  return String(value ?? '').toLowerCase().trim();
}

/**
 * Filters projects by a free-text query against project name or category.
 *
 * @param {Array<{name?: string, category?: string}>} projects
 * @param {string} query
 * @returns {Array<{name?: string, category?: string}>}
 */
export function filterProjects(projects, query) {
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
