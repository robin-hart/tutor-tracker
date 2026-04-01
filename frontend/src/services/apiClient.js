const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Executes an HTTP request against the TutorTimeTracker backend.
 *
 * @param {string} path endpoint path after `/api`
 * @param {RequestInit} [options] fetch options
 * @returns {Promise<any>} resolved JSON payload
 */
async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (options.headers) {
    Object.assign(headers, options.headers);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers,
    ...options,
  });

  if (!response.ok) {
    let apiMessage = '';
    try {
      const errorBody = await response.json();
      apiMessage = errorBody.message ? ` - ${errorBody.message}` : '';
    } catch {
      // Ignore non-JSON error bodies and keep default empty API message.
    }
    throw new Error(`API request failed: ${response.status} ${response.statusText}${apiMessage}`);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    return null;
  }

  return response.json();
}

/** @returns {Promise<any[]>} */
export function getProjects() {
  return request('/projects');
}

/**
 * @param {{name: string, category: string, totalHours: number, monthHours: number, completionPercent: number}} payload
 */
export function createProject(payload) {
  return request('/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * @param {string} projectId
 */
export function deleteProject(projectId) {
  return request(`/projects/${projectId}`, {
    method: 'DELETE',
  });
}

/** @param {string} projectId @returns {Promise<any>} */
export function getProjectCalendar(projectId, month) {
  const query = month ? `?month=${encodeURIComponent(month)}` : '';
  return request(`/projects/${projectId}/calendar${query}`);
}

/** @param {string} projectId @returns {Promise<any[]>} */
export function getProjectStudents(projectId) {
  return request(`/projects/${projectId}/students`);
}

/** @param {string} projectId @returns {Promise<any[]>} */
export function getProjectGroups(projectId) {
  return request(`/projects/${projectId}/groups`);
}

/**
 * @param {string} projectId
 * @param {string} name
 */
export function createProjectGroup(projectId, name) {
  return request(`/projects/${projectId}/groups`, {
    method: 'POST',
    body: JSON.stringify({ name }),
  });
}

/**
 * @param {string} projectId
 * @param {string} groupName
 */
export function deleteProjectGroup(projectId, groupName) {
  return request(`/projects/${projectId}/groups/${encodeURIComponent(groupName)}`, {
    method: 'DELETE',
  });
}

/**
 * @param {string} projectId
 * @param {{name: string, notes: string, groupName?: string}} payload
 */
export function createProjectStudent(projectId, payload) {
  return request(`/projects/${projectId}/students`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * @param {string} studentId
 * @param {string} notes
 */
export function updateStudentNotes(studentId, notes) {
  return request(`/students/${studentId}/notes`, {
    method: 'PATCH',
    body: JSON.stringify({ notes }),
  });
}

/**
 * @param {string} studentId
 * @param {string} groupName
 */
export function updateStudentGroup(studentId, groupName) {
  return request(`/students/${studentId}/group`, {
    method: 'PATCH',
    body: JSON.stringify({ groupName }),
  });
}

/**
 * @param {string} studentId
 * @returns {Promise<null>}
 */
export function deleteStudent(studentId) {
  return request(`/students/${studentId}`, {
    method: 'DELETE',
  });
}

/** @returns {Promise<any[]>} */
export function getReports() {
  return request('/reports');
}

/** @param {string} projectId */
export function getProjectReports(projectId) {
  return request(`/projects/${projectId}/reports`);
}

/**
 * @param {string} projectId
 * @param {string} month yyyy-MM
 */
export function generateProjectReport(projectId, month) {
  return request(`/projects/${projectId}/reports/generate?month=${encodeURIComponent(month)}`, {
    method: 'POST',
  });
}

/**
 * @param {string} projectId
 * @param {string} month yyyy-MM
 * @returns {Promise<Blob>}
 */
export async function exportProjectReportPdf(projectId, month) {
  const response = await fetch(
    `${API_BASE_URL}/projects/${projectId}/reports/export/pdf?month=${encodeURIComponent(month)}`
  );

  if (!response.ok) {
    let apiMessage = '';
    try {
      const errorBody = await response.json();
      apiMessage = errorBody.message ? ` - ${errorBody.message}` : '';
    } catch {
      // Ignore non-JSON error bodies and keep default empty API message.
    }
    throw new Error(`API request failed: ${response.status} ${response.statusText}${apiMessage}`);
  }

  return response.blob();
}

/**
 * Saves a timeslot payload.
 *
 * @param {string} projectId
 * @param {{title: string, description: string, durationMinutes: number, date: string, startTime: string}} payload
 * @returns {Promise<any>}
 */
export function saveTimeslot(projectId, payload) {
  return request(`/projects/${projectId}/timeslots`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

/**
 * @param {string} projectId
 * @param {string} timeslotId
 * @returns {Promise<any>}
 */
export function getProjectTimeslot(projectId, timeslotId) {
  return request(`/projects/${projectId}/timeslots/${timeslotId}`);
}

/**
 * @param {string} projectId
 * @param {string} timeslotId
 * @param {{title: string, description: string, durationMinutes: number, date: string, startTime: string}} payload
 * @returns {Promise<any>}
 */
export function updateProjectTimeslot(projectId, timeslotId, payload) {
  return request(`/projects/${projectId}/timeslots/${timeslotId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

/**
 * @param {string} projectId
 * @param {string} timeslotId
 * @returns {Promise<null>}
 */
export function deleteProjectTimeslot(projectId, timeslotId) {
  return request(`/projects/${projectId}/timeslots/${timeslotId}`, {
    method: 'DELETE',
  });
}
