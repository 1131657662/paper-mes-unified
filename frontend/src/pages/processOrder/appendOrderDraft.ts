import type { ProcessPlanDTO } from '../../types/processOrder'
import type { RollDraft } from '../../features/processOrderCreate/types'

const STORAGE_PREFIX = 'paper-mes:process-order-append:'

export interface AppendOrderDraft {
  current: number
  plans: Record<string, ProcessPlanDTO>
  rolls: RollDraft[]
  savedAt: string
  selectedId?: string
  sessionUuid: string
  sessionVersion: number
}

export function readAppendOrderDraft(sessionUuid: string): AppendOrderDraft | null {
  try {
    const raw = localStorage.getItem(storageKey(sessionUuid))
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    if (!isDraft(parsed) || parsed.sessionUuid !== sessionUuid) return null
    return parsed
  } catch {
    return null
  }
}

export function writeAppendOrderDraft(draft: AppendOrderDraft): void {
  try {
    localStorage.setItem(storageKey(draft.sessionUuid), JSON.stringify(draft))
  } catch {
    // Server-side saves remain available when browser storage is unavailable.
  }
}

export function clearAppendOrderDraft(sessionUuid: string): void {
  try {
    localStorage.removeItem(storageKey(sessionUuid))
  } catch {
    // Clearing a submitted draft is best effort.
  }
}

function storageKey(sessionUuid: string): string {
  return `${STORAGE_PREFIX}${sessionUuid}`
}

function isDraft(value: unknown): value is AppendOrderDraft {
  if (!value || typeof value !== 'object') return false
  const draft = value as Partial<AppendOrderDraft>
  return typeof draft.sessionUuid === 'string'
    && typeof draft.sessionVersion === 'number'
    && typeof draft.current === 'number'
    && typeof draft.savedAt === 'string'
    && Array.isArray(draft.rolls)
    && Boolean(draft.plans) && typeof draft.plans === 'object'
}
