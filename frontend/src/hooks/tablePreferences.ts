import type { ColumnsState } from '@ant-design/pro-table/es/Store/Provide'

export type ColumnsStateMap = Record<string, ColumnsState>

export interface TablePreferences<TSort> {
  version: 2
  columns: ColumnsStateMap
  sortChain: TSort[]
  sortChains: Record<string, TSort[]>
}

const CURRENT_VERSION = 2

export function readTablePreferences<TSort>(storageKey: string): TablePreferences<TSort> {
  try {
    const saved = localStorage.getItem(storageKey)
    if (!saved) return emptyPreferences<TSort>()
    const parsed: unknown = JSON.parse(saved)
    if (isEnvelope<TSort>(parsed)) {
      return {
        version: CURRENT_VERSION,
        columns: parsed.columns,
        sortChain: Array.isArray(parsed.sortChain) ? parsed.sortChain : [],
        sortChains: isSortChainsMap(parsed.sortChains) ? parsed.sortChains as Record<string, TSort[]> : {},
      }
    }
    return {
      version: CURRENT_VERSION,
      columns: isColumnsStateMap(parsed) ? parsed : {},
      sortChain: [],
      sortChains: {},
    }
  } catch (error) {
    console.warn(`Failed to load table preferences for ${storageKey}:`, error)
    return emptyPreferences<TSort>()
  }
}

export function updateTablePreferences<TSort>(
  storageKey: string,
  update: (current: TablePreferences<TSort>) => TablePreferences<TSort>,
) {
  try {
    const next = update(readTablePreferences<TSort>(storageKey))
    localStorage.setItem(storageKey, JSON.stringify({ ...next, version: CURRENT_VERSION }))
  } catch (error) {
    console.warn(`Failed to save table preferences for ${storageKey}:`, error)
  }
}

function emptyPreferences<TSort>(): TablePreferences<TSort> {
  return { version: CURRENT_VERSION, columns: {}, sortChain: [], sortChains: {} }
}

function isEnvelope<TSort>(value: unknown): value is TablePreferences<TSort> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  const record = value as Record<string, unknown>
  return isColumnsStateMap(record.columns)
}

function isColumnsStateMap(value: unknown): value is ColumnsStateMap {
  return Boolean(value && typeof value === 'object' && !Array.isArray(value))
}

function isSortChainsMap(value: unknown): value is Record<string, unknown[]> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return false
  return Object.values(value).every((chain) => Array.isArray(chain))
}
