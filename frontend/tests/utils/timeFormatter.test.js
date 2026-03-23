import { describe, it, expect } from 'vitest';
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

    it('should convert 48.5 hours to 48 hours and 30 minutes', () => {
      const result = formatHoursToHM(48.5);
      expect(result).toEqual({ hours: 48, minutes: 30 });
    });

    it('should convert 0.5 hours to 0 hours and 30 minutes', () => {
      const result = formatHoursToHM(0.5);
      expect(result).toEqual({ hours: 0, minutes: 30 });
    });

    it('should convert 1.5 hours to 1 hour and 30 minutes', () => {
      const result = formatHoursToHM(1.5);
      expect(result).toEqual({ hours: 1, minutes: 30 });
    });

    it('should convert 12.75 hours to 12 hours and 45 minutes', () => {
      const result = formatHoursToHM(12.75);
      expect(result).toEqual({ hours: 12, minutes: 45 });
    });
  });

  describe('integer values', () => {
    it('should convert 5 hours to 5 hours and 0 minutes', () => {
      const result = formatHoursToHM(5);
      expect(result).toEqual({ hours: 5, minutes: 0 });
    });

    it('should convert 1 hour to 1 hour and 0 minutes', () => {
      const result = formatHoursToHM(1);
      expect(result).toEqual({ hours: 1, minutes: 0 });
    });

    it('should convert 0 hours to 0 hours and 0 minutes', () => {
      const result = formatHoursToHM(0);
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });

    it('should convert 100 hours to 100 hours and 0 minutes', () => {
      const result = formatHoursToHM(100);
      expect(result).toEqual({ hours: 100, minutes: 0 });
    });
  });

  describe('rounding behavior', () => {
    it('should round 0.5 to 30 minutes', () => {
      const result = formatHoursToHM(0.5);
      expect(result.minutes).toBe(30);
    });

    it('should round 0.516667 to 31 minutes', () => {
      const result = formatHoursToHM(1.516667);
      expect(result).toEqual({ hours: 1, minutes: 31 });
    });

    it('should round 0.583333 to 35 minutes', () => {
      const result = formatHoursToHM(1.583333);
      expect(result).toEqual({ hours: 1, minutes: 35 });
    });

    it('should round 0.99 to 59 minutes', () => {
      const result = formatHoursToHM(0.99);
      expect(result).toEqual({ hours: 0, minutes: 59 });
    });

    it('should round 0.005 to 0 minutes', () => {
      const result = formatHoursToHM(0.005);
      expect(result).toEqual({ hours: 0, minutes: 0 });
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

    it('should handle NaN as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM(NaN);
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });

    it('should handle empty string as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM('');
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });
  });

  describe('string input conversions', () => {
    it('should convert string "2.25" to 2 hours and 15 minutes', () => {
      const result = formatHoursToHM('2.25');
      expect(result).toEqual({ hours: 2, minutes: 15 });
    });

    it('should convert string "5" to 5 hours and 0 minutes', () => {
      const result = formatHoursToHM('5');
      expect(result).toEqual({ hours: 5, minutes: 0 });
    });

    it('should convert string "0.5" to 0 hours and 30 minutes', () => {
      const result = formatHoursToHM('0.5');
      expect(result).toEqual({ hours: 0, minutes: 30 });
    });

    it('should handle invalid string as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM('invalid');
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });

    it('should handle whitespace string as 0 hours and 0 minutes', () => {
      const result = formatHoursToHM('   ');
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });
  });

  describe('negative values', () => {
    it('should handle negative values by treating as positive after Number conversion', () => {
      const result = formatHoursToHM(-2.25);
      expect(result.hours).toBe(-3); // Math.floor(-2.25) = -3
      expect(result.minutes).toBeLessThanOrEqual(60);
    });

    it('should handle -1 as -1 hours and 0 minutes', () => {
      const result = formatHoursToHM(-1);
      expect(result).toEqual({ hours: -1, minutes: 0 });
    });
  });

  describe('very small decimal values', () => {
    it('should convert 0.01 hours to 0 hours and 1 minute', () => {
      const result = formatHoursToHM(0.01);
      expect(result).toEqual({ hours: 0, minutes: 1 });
    });

    it('should convert 0.001 hours to 0 hours and 0 minutes', () => {
      const result = formatHoursToHM(0.001);
      expect(result).toEqual({ hours: 0, minutes: 0 });
    });

    it('should convert 0.0167 hours to 0 hours and 1 minute', () => {
      const result = formatHoursToHM(0.0167);
      expect(result).toEqual({ hours: 0, minutes: 1 });
    });
  });

  describe('very large values', () => {
    it('should convert 999.5 hours to 999 hours and 30 minutes', () => {
      const result = formatHoursToHM(999.5);
      expect(result).toEqual({ hours: 999, minutes: 30 });
    });

    it('should convert 10000 hours to 10000 hours and 0 minutes', () => {
      const result = formatHoursToHM(10000);
      expect(result).toEqual({ hours: 10000, minutes: 0 });
    });
  });

  describe('return value structure', () => {
    it('should always return an object with hours and minutes properties', () => {
      const result = formatHoursToHM(2.5);
      expect(result).toHaveProperty('hours');
      expect(result).toHaveProperty('minutes');
      expect(Object.keys(result)).toEqual(['hours', 'minutes']);
    });

    it('should always return numbers for hours and minutes', () => {
      const result = formatHoursToHM(3.75);
      expect(typeof result.hours).toBe('number');
      expect(typeof result.minutes).toBe('number');
    });

    it('should return integer values for hours and minutes', () => {
      const result = formatHoursToHM(2.25);
      expect(Number.isInteger(result.hours)).toBe(true);
      expect(Number.isInteger(result.minutes)).toBe(true);
    });

    it('should never return minutes greater than 59', () => {
      // Test various values that could cause rounding to exceed 60
      const testValues = [0.9999, 1.9999, 2.9999, 0.99999];
      testValues.forEach((value) => {
        const result = formatHoursToHM(value);
        expect(result.minutes).toBeLessThan(60);
      });
    });
  });

  describe('practical use cases', () => {
    it('should convert a typical tutoring session (1.75 hours)', () => {
      const result = formatHoursToHM(1.75);
      expect(result).toEqual({ hours: 1, minutes: 45 });
    });

    it('should convert a standard work day (8 hours)', () => {
      const result = formatHoursToHM(8);
      expect(result).toEqual({ hours: 8, minutes: 0 });
    });

    it('should convert a work week (40 hours)', () => {
      const result = formatHoursToHM(40);
      expect(result).toEqual({ hours: 40, minutes: 0 });
    });

    it('should convert a short break (0.25 hours / 15 minutes)', () => {
      const result = formatHoursToHM(0.25);
      expect(result).toEqual({ hours: 0, minutes: 15 });
    });

    it('should convert a month of tutoring (160.5 hours)', () => {
      const result = formatHoursToHM(160.5);
      expect(result).toEqual({ hours: 160, minutes: 30 });
    });
  });
});
