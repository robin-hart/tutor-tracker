export interface HourMinute {
  hours: number;
  minutes: number;
}

export interface Project {
  id: string;
  name: string;
  category?: string;
  institution?: string;
  totalHours?: number;
  monthHours?: number;
  targetMonthHours?: number;
  completionPercent?: number;
}

export interface ProjectPayload {
  name: string;
  category: string;
  institution: string;
  targetMonthHours: number;
  completionPercent: number;
}

export interface Timeslot {
  id: string;
  title: string;
  description?: string;
  durationMinutes: number;
  date: string;
  startTime: string;
}

export interface CalendarPayload {
  projectName: string;
  totalHours: number;
  monthHours: number;
  monthSlots: Timeslot[];
  allSlots?: Timeslot[];
}

export interface Student {
  id: string;
  name: string;
  notes?: string;
  groupName?: string;
}

export interface StudentPayload {
  name: string;
  notes: string;
  groupName?: string;
}

export interface Group {
  name: string;
}

export interface Report {
  id: string;
  projectId: string;
  month: string;
}

export interface TimeslotPayload {
  title: string;
  description: string;
  durationMinutes: number;
  date: string;
  startTime: string;
}
