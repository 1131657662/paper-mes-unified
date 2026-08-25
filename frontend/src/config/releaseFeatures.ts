/**
 * Frontend-only release switches for unfinished user-facing modules.
 * Undefined environment values intentionally keep these features disabled.
 */
export const RELEASE_FEATURES = {
  remainModuleEnabled: import.meta.env.VITE_REMAIN_MODULE_ENABLED === 'true',
  aiButtonsEnabled: import.meta.env.VITE_AI_BUTTONS_ENABLED === 'true',
} as const
