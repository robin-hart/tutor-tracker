import { describe, it, expect } from 'vitest';
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

    it('matches category for query "a" when category contains "a"', () => {
      const result = filterProjects(sampleProjects, 'a');
      expect(result.some((p) => p.name === 'FSB')).toBe(true);
    });

    it('returns no projects when query matches neither name nor category', () => {
      const result = filterProjects(sampleProjects, 'zzz');
      expect(result).toHaveLength(0);
    });

    it('handles missing name/category fields safely', () => {
      const result = filterProjects([{ id: 'x' }, { id: 'y', name: 'Alpha' }], 'alpha');
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe('y');
    });
  });
});
