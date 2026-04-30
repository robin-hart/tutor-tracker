import { ref, readonly } from 'vue';

/**
 * Composable for managing user settings persisted in localStorage.
 */

const STORAGE_KEY = 'tutorflow_user_name';

const userName = ref<string>(loadUserName());

/**
 * Load user name from localStorage.
 */
function loadUserName(): string {
  if (typeof window === 'undefined') return '';
  return localStorage.getItem(STORAGE_KEY) || '';
}

/**
 * Save user name to localStorage.
 */
function saveUserName(name: string): void {
  userName.value = name;
  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, name);
  }
}

/**
 * Clear user name from localStorage.
 */
function clearUserName(): void {
  userName.value = '';
  if (typeof window !== 'undefined') {
    localStorage.removeItem(STORAGE_KEY);
  }
}

export function useUserSettings() {
  return {
    userName: readonly(userName),
    saveUserName,
    clearUserName,
  };
}
