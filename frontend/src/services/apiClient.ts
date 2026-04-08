import type {
  CalendarPayload,
  Group,
  Project,
  ProjectPayload,
  Report,
  Student,
  StudentPayload,
  Timeslot,
  TimeslotPayload,
} from '../types/domain';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Executes an HTTP request against the TutorTimeTracker backend.
 */
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (options.headers) {
    Object.assign(headers, options.headers as Record<string, string>);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers,
    ...options,
  });

  if (!response.ok) {
    let apiMessage = '';
    try {
      const errorBody = (await response.json()) as { message?: string };
      apiMessage = errorBody.message ? ` - ${errorBody.message}` : '';
    } catch {
      // Ignore non-JSON error bodies and keep default empty API message.
    }
    throw new Error(`API request failed: ${response.status} ${response.statusText}${apiMessage}`);
  }

  if (response.status === 204) {
    return null as T;
  }

  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    return null as T;
  }

  return (await response.json()) as T;
}

export function getProjects(): Promise<Project[]> {
  return request<Project[]>('/projects');
}

export function createProject(payload: ProjectPayload): Promise<Project> {
  return request<Project>('/projects', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateProject(projectId: string, payload: ProjectPayload): Promise<Project> {
  return request<Project>(`/projects/${projectId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteProject(projectId: string): Promise<null> {
  return request<null>(`/projects/${projectId}`, {
    method: 'DELETE',
  });
}

export function getProjectCalendar(projectId: string, month?: string): Promise<CalendarPayload> {
  const query = month ? `?month=${encodeURIComponent(month)}` : '';
  return request<CalendarPayload>(`/projects/${projectId}/calendar${query}`);
}

export function getProjectStudents(projectId: string): Promise<Student[]> {
  return request<Student[]>(`/projects/${projectId}/students`);
}

export function getProjectGroups(projectId: string): Promise<Group[]> {
  return request<Group[]>(`/projects/${projectId}/groups`);
}

export function createProjectGroup(projectId: string, name: string): Promise<Group> {
  return request<Group>(`/projects/${projectId}/groups`, {
    method: 'POST',
    body: JSON.stringify({ name }),
  });
}

export function deleteProjectGroup(projectId: string, groupName: string): Promise<null> {
  return request<null>(`/projects/${projectId}/groups/${encodeURIComponent(groupName)}`, {
    method: 'DELETE',
  });
}

export function createProjectStudent(projectId: string, payload: StudentPayload): Promise<Student> {
  return request<Student>(`/projects/${projectId}/students`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function updateStudentNotes(studentId: string, notes: string): Promise<Student> {
  return request<Student>(`/students/${studentId}/notes`, {
    method: 'PATCH',
    body: JSON.stringify({ notes }),
  });
}

export function updateStudentGroup(studentId: string, groupName: string): Promise<Student> {
  return request<Student>(`/students/${studentId}/group`, {
    method: 'PATCH',
    body: JSON.stringify({ groupName }),
  });
}

export function deleteStudent(studentId: string): Promise<null> {
  return request<null>(`/students/${studentId}`, {
    method: 'DELETE',
  });
}

export function getReports(): Promise<Report[]> {
  return request<Report[]>('/reports');
}

export function getProjectReports(projectId: string): Promise<Report[]> {
  return request<Report[]>(`/projects/${projectId}/reports`);
}

export function generateProjectReport(projectId: string, month: string): Promise<Report> {
  return request<Report>(
    `/projects/${projectId}/reports/generate?month=${encodeURIComponent(month)}`,
    {
      method: 'POST',
    }
  );
}

export async function exportProjectReportPdf(projectId: string, month: string): Promise<Blob> {
  const response = await fetch(
    `${API_BASE_URL}/projects/${projectId}/reports/export/pdf?month=${encodeURIComponent(month)}`
  );

  if (!response.ok) {
    let apiMessage = '';
    try {
      const errorBody = (await response.json()) as { message?: string };
      apiMessage = errorBody.message ? ` - ${errorBody.message}` : '';
    } catch {
      // Ignore non-JSON error bodies and keep default empty API message.
    }
    throw new Error(`API request failed: ${response.status} ${response.statusText}${apiMessage}`);
  }

  return response.blob();
}

export function saveTimeslot(projectId: string, payload: TimeslotPayload): Promise<Timeslot> {
  return request<Timeslot>(`/projects/${projectId}/timeslots`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getProjectTimeslot(projectId: string, timeslotId: string): Promise<Timeslot> {
  return request<Timeslot>(`/projects/${projectId}/timeslots/${timeslotId}`);
}

export function updateProjectTimeslot(
  projectId: string,
  timeslotId: string,
  payload: TimeslotPayload
): Promise<Timeslot> {
  return request<Timeslot>(`/projects/${projectId}/timeslots/${timeslotId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export function deleteProjectTimeslot(projectId: string, timeslotId: string): Promise<null> {
  return request<null>(`/projects/${projectId}/timeslots/${timeslotId}`, {
    method: 'DELETE',
  });
}
