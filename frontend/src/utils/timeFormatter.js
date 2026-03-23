/**
 * Converts decimal hours to hours and minutes components
 * @param {number} decimalHours - Hours as a decimal (e.g., 2.25)
 * @returns {{hours: number, minutes: number}} Object with hours and minutes
 */
export function formatHoursToHM(decimalHours) {
  const value = Number(decimalHours) || 0;
  if (isNaN(value)) {
    return { hours: 0, minutes: 0 };
  }

  let hours = Math.floor(value);
  let minutes = Math.round((value - hours) * 60);

  // Handle edge case where rounding produces 60 minutes
  if (minutes === 60) {
    hours += 1;
    minutes = 0;
  }

  return { hours, minutes };
}
