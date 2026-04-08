import { describe, expect, it } from 'vitest';
import { formatHoursToHM } from '../../src/utils/timeFormatter';

describe('formatHoursToHM', () => {
  describe('basic conversions', () => {
    it('should convert 2.25 hours to 2 hours and 15 minutes', () => {
      const result = formatHoursToHM(2.25);
      expect(result).toEqual({ hours: 2, minutes: 15 });
    });

    it('should convert 2.3 hours to 2 hours and 18 minutes', () => {
      const result = formatHoursToHM(2.3);
      expect(result).toEqual({ hours: 2, minutes: 18 });
    });

    it('should convert 2.45 hours to 2 hours and 27 minutes', () => {
      const result = formatHoursToHM(2.45);
      expect(result).toEqual({ hours: 2, minutes: 27 });
    });
  });

  describe('edge cases', () => {
    it('should handle undefined as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM(undefined);
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });

    it('should handle null as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM(null);
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });

    it('should handle invalid string as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM('invalid');
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });
  });
});
