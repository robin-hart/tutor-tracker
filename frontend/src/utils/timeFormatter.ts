import type { HourMinute } from '../types/domain';

/**
 * Converts decimal hours to hour/minute parts.
 */
export function formatHoursToHM(decimalHours: unknown): HourMinute {
  const value = Number(decimalHours) || 0;
  if (Number.isNaN(value)) {
    return { hours: 0, minutes: 0 };
  }

  let hours = Math.floor(value);
  let minutes = Math.round((value - hours) * 60);

  if (minutes === 60) {
    hours += 1;
    minutes = 0;
  }

  return { hours, minutes };
}
