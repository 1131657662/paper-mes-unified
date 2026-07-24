import type {
  DraftOrderBaseDTO,
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../types/processOrder'
import type { RollDraft } from './types'

const STORAGE_KEY = 'paper-mes:create-order-local-draft:v1'
const VERSION = 1
const MAX_DRAFT_AGE_MS = 7 * 24 * 60 * 60 * 1000

export interface CreateOrderLocalDraftInput {
  baseInfo?: DraftOrderBaseDTO
  current: number
  configuredPlanIds: string[]
  orderUuid?: string
  orderVersion?: number
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  routes: Record<string, ProcessRoutePreviewDTO>
  rolls: RollDraft[]
  selectedId?: string
}

export interface CreateOrderLocalDraft extends CreateOrderLocalDraftInput {
  savedAt: string
  version: typeof VERSION
}

export function loadCreateOrderLocalDraft(): CreateOrderLocalDraft | undefined {
  const storage = safeStorage()
  if (!storage) return undefined
  const rawValue = storage.getItem(STORAGE_KEY)
  if (!rawValue) return undefined

  try {
    const draft = parseLocalDraft(JSON.parse(rawValue) as unknown)
    if (!draft || isExpired(draft.savedAt)) {
      storage.removeItem(STORAGE_KEY)
      return undefined
    }
    return draft
  } catch {
    storage.removeItem(STORAGE_KEY)
    return undefined
  }
}

function isExpired(savedAt: string) {
  const savedTime = Date.parse(savedAt)
  return !Number.isFinite(savedTime) || Date.now() - savedTime > MAX_DRAFT_AGE_MS
}

export function saveCreateOrderLocalDraft(input: CreateOrderLocalDraftInput) {
  const storage = safeStorage()
  if (!storage) return
  if (!hasMeaningfulDraft(input)) {
    storage.removeItem(STORAGE_KEY)
    return
  }

  const draft: CreateOrderLocalDraft = {
    ...input,
    savedAt: new Date().toISOString(),
    version: VERSION,
  }

  try {
    storage.setItem(STORAGE_KEY, JSON.stringify(draft))
  } catch {
    // Local draft is a convenience only; failing to write must not block order creation.
  }
}

export function clearCreateOrderLocalDraft() {
  safeStorage()?.removeItem(STORAGE_KEY)
}

function parseLocalDraft(value: unknown): CreateOrderLocalDraft | undefined {
  if (!isRecord(value) || value.version !== VERSION || !isRollDraftArray(value.rolls)) {
    return undefined
  }

  return {
    baseInfo: isBaseInfo(value.baseInfo) ? value.baseInfo : undefined,
    current: typeof value.current === 'number' ? value.current : 0,
    configuredPlanIds: isStringArray(value.configuredPlanIds) ? value.configuredPlanIds : [],
    orderUuid: optionalString(value.orderUuid),
    orderVersion: optionalNumber(value.orderVersion),
    plans: isRecord(value.plans) ? (value.plans as Record<string, ProcessPlanDTO>) : {},
    previews: isRecord(value.previews) ? (value.previews as Record<string, PlanPreviewVO>) : {},
    routePreviews: isRecord(value.routePreviews) ? (value.routePreviews as Record<string, ProcessRoutePreviewVO>) : {},
    routes: isRecord(value.routes) ? (value.routes as Record<string, ProcessRoutePreviewDTO>) : {},
    rolls: value.rolls,
    savedAt: typeof value.savedAt === 'string' ? value.savedAt : '',
    selectedId: optionalString(value.selectedId),
    version: VERSION,
  }
}

function hasMeaningfulDraft(input: CreateOrderLocalDraftInput) {
  return Boolean(
    input.orderUuid
    || input.current > 0
    || hasMeaningfulBaseInfo(input.baseInfo)
    || input.rolls.some(hasMeaningfulRoll),
  )
}

function hasMeaningfulBaseInfo(baseInfo?: DraftOrderBaseDTO) {
  if (!baseInfo) return false
  return Object.values(baseInfo).some((value) => value !== undefined && value !== null && value !== '')
}

function hasMeaningfulRoll(roll: RollDraft) {
  return Boolean(
    roll.uuid
    || roll.paperName?.trim()
    || roll.rollNo?.trim()
    || roll.extraNo?.trim()
    || roll.remark?.trim()
    || Number(roll.rollWeight ?? 0) > 0
    || roll.gramWeight !== 80
    || roll.originalWidth !== 1000,
  )
}

function isRollDraftArray(value: unknown): value is RollDraft[] {
  return Array.isArray(value)
    && value.length > 0
    && value.every((item) => isRecord(item) && typeof item.localId === 'string')
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isBaseInfo(value: unknown): value is DraftOrderBaseDTO {
  return isRecord(value)
    && typeof value.customerUuid === 'string'
    && typeof value.orderDate === 'string'
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function optionalString(value: unknown) {
  return typeof value === 'string' && value ? value : undefined
}

function optionalNumber(value: unknown) {
  return typeof value === 'number' && Number.isInteger(value) && value >= 0 ? value : undefined
}

function safeStorage() {
  try {
    return typeof window === 'undefined' ? undefined : window.localStorage
  } catch {
    return undefined
  }
}
