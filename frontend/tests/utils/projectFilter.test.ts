import { describe, expect, it } from 'vitest';
import { filterProjects, normalizeForProjectSearch } from '../../src/utils/projectFilter';

describe('projectFilter', () => {
  describe('normalizeForProjectSearch', () => {
    it('normalizes text to lower case and trims', () => {
      expect(normalizeForProjectSearch('  FSB  ')).toBe('fsb');
    });

    it('handles null and undefined safely', () => {
      expect(normalizeForProjectSearch(null)).toBe('');
      expect(normalizeForProjectSearch(undefined)).toBe('');
    });
  });

  describe('filterProjects', () => {
    const sampleProjects = [
      { id: '1', name: 'FSB', category: 'GENERAL' },
      { id: '2', name: 'Math Grade 10', category: 'STEM' },
      { id: '3', name: 'Physics University', category: 'SCIENCE' },
    ];

    it('returns all projects for empty query', () => {
      const result = filterProjects(sampleProjects, '');
      expect(result).toHaveLength(3);
    });

    it('filters by name case-insensitively', () => {
      const result = filterProjects(sampleProjects, 'fsb');
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('FSB');
    });

    it('filters by category case-insensitively', () => {
      const result = filterProjects(sampleProjects, 'stem');
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('Math Grade 10');
    });
  });
});
