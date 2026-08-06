import type { BackRecordFormValues } from './backRecordUtils'

const SCHEMA_VERSION = 1
const STORAGE_PREFIX = 'paper-mes:back-record-draft:'

export interface BackRecordDraft {
  orderUuid: string
  orderVersion?: number
  savedAt: string
  values: BackRecordFormValues
}

export function readBackRecordDraft(orderUuid: string): BackRecordDraft | null {
  try {
    const raw = localStorage.getItem(storageKey(orderUuid))
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    if (!isDraftEnvelope(parsed) || parsed.orderUuid !== orderUuid) return null
    return {
      orderUuid: parsed.orderUuid,
      orderVersion: parsed.orderVersion,
      savedAt: parsed.savedAt,
      values: parsed.values as BackRecordFormValues,
    }
  } catch {
    return null
  }
}

export function writeBackRecordDraft(
  orderUuid: string,
  orderVersion: number | undefined,
  values: BackRecordFormValues,
): void {
  const draft = {
    schemaVersion: SCHEMA_VERSION,
    orderUuid,
    orderVersion,
    savedAt: new Date().toISOString(),
    values,
  }
  try {
    localStorage.setItem(storageKey(orderUuid), JSON.stringify(draft))
  } catch {
    // Form submission remains available when browser storage is unavailable.
  }
}

export function clearBackRecordDraft(orderUuid: string): void {
  try {
    localStorage.removeItem(storageKey(orderUuid))
  } catch {
    // Clearing browser storage is best effort after the server has persisted data.
  }
}

function storageKey(orderUuid: string): string {
  return `${STORAGE_PREFIX}${orderUuid}`
}

function isDraftEnvelope(value: unknown): value is Record<string, unknown> & {
  orderUuid: string
  orderVersion?: number
  savedAt: string
  values: Record<string, unknown>
} {
  if (!value || typeof value !== 'object') return false
  const draft = value as Record<string, unknown>
  return draft.schemaVersion === SCHEMA_VERSION
    && typeof draft.orderUuid === 'string'
    && (draft.orderVersion == null || typeof draft.orderVersion === 'number')
    && typeof draft.savedAt === 'string'
    && Boolean(draft.values) && typeof draft.values === 'object'
}
