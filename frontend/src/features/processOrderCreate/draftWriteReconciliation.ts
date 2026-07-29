import { message } from 'antd'
import type { QueryClient } from '@tanstack/react-query'
import { isUncertainRequestError, notifyErrorOnce } from '../../api/request'
import { queries } from '../../queries'
import type {
  DraftOrderVO,
  DraftRollProcessBatchSaveDTO,
  PlanPreviewVO,
  ProcessPlanBatchSaveDTO,
  ProcessPlanItemsBatchSaveDTO,
  ProcessPlanPreviewRequestDTO,
  ProcessRouteBatchSaveDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../types/processOrder'
import { createOrderService } from './services/createOrderService'
import type { VersionedWriteResult } from './draftWriteTypes'

const CONFIRM_DELAYS_MS = [0, 500, 1_000, 2_000, 4_000, 8_000] as const

interface ReconciledWriteOptions<T> {
  expectedVersion: number
  isApplied: (draft: DraftOrderVO) => boolean
  readLatest: () => Promise<DraftOrderVO>
  recoverData: (draft: DraftOrderVO) => T
  write: () => Promise<T>
}

export async function runReconciledDraftWrite<T>(
  options: ReconciledWriteOptions<T>,
): Promise<VersionedWriteResult<T>> {
  try {
    return successfulWrite(await options.write(), options.expectedVersion)
  } catch (error) {
    if (!isUncertainRequestError(error)) throw error
    const latest = await confirmUncertainWrite(options)
    if (!latest) return rejectUnconfirmedWrite(error)
    const version = Number(latest.order?.version ?? 0)
    message.success('响应异常，但已从服务器确认保存成功')
    return { data: options.recoverData(latest), recovered: true, version }
  }
}

export async function readLatestDraft(
  queryClient: QueryClient,
  orderUuid: string,
): Promise<DraftOrderVO> {
  const draft = await createOrderService.draftForRecovery(orderUuid)
  queryClient.setQueryData(queries.createOrder.draft(orderUuid).queryKey, draft)
  return draft
}

export function progressMatches(draft: DraftOrderVO, currentStep: number): boolean {
  return draft.currentStep === currentStep
}

export function rollProcessesMatch(
  draft: DraftOrderVO,
  request: DraftRollProcessBatchSaveDTO,
): boolean {
  const rolls = new Map((draft.rolls ?? []).map((roll) => [roll.uuid, roll]))
  return request.rolls.every((expected) => {
    const actual = rolls.get(expected.originalUuid)
    return actual !== undefined
      && actual.processMode === expected.processMode
      && optionalValueMatches(actual.mainStepType, expected.mainStepType)
      && optionalValueMatches(actual.machineUuid, expected.machineUuid)
  })
}

export function singlePlanMatches(
  draft: DraftOrderVO,
  request: ProcessPlanPreviewRequestDTO,
): boolean {
  return planConfigMatches(draft, request.originalUuid, request.plan)
}

export function batchPlanMatches(draft: DraftOrderVO, request: ProcessPlanBatchSaveDTO): boolean {
  return request.originalUuids.every((uuid) => planConfigMatches(draft, uuid, request.plan))
}

export function itemPlansMatch(
  draft: DraftOrderVO,
  request: ProcessPlanItemsBatchSaveDTO,
): boolean {
  return request.items.every((item) => planConfigMatches(draft, item.originalUuid, item.plan))
}

export function recoverPlanPreviews(draft: DraftOrderVO, originalUuids: string[]): PlanPreviewVO[] {
  return originalUuids.flatMap((uuid) => {
    const preview = draft.configs?.find((config) => config.originalUuid === uuid)?.preview
    return preview ? [preview] : []
  })
}

export function singleRouteMatches(draft: DraftOrderVO, request: ProcessRoutePreviewDTO): boolean {
  return routeConfigMatches(draft, request)
}

export function batchRoutesMatch(draft: DraftOrderVO, request: ProcessRouteBatchSaveDTO): boolean {
  return request.routes.every((route) => routeConfigMatches(draft, route))
}

export function recoverRoutePreviews(
  draft: DraftOrderVO,
  originalUuids: string[],
): ProcessRoutePreviewVO[] {
  return originalUuids.flatMap((uuid) => {
    const preview = draft.configs?.find((config) => config.originalUuid === uuid)?.routePreview
    return preview ? [preview] : []
  })
}

async function confirmUncertainWrite<T>(
  options: ReconciledWriteOptions<T>,
): Promise<DraftOrderVO | undefined> {
  for (const delayMs of CONFIRM_DELAYS_MS) {
    await waitForConfirmation(delayMs)
    const latest = await readLatestOrUndefined(options.readLatest)
    const version = Number(latest?.order?.version ?? 0)
    if (latest && version > options.expectedVersion && options.isApplied(latest)) return latest
  }
  return undefined
}

async function readLatestOrUndefined(
  readLatest: () => Promise<DraftOrderVO>,
): Promise<DraftOrderVO | undefined> {
  try {
    return await readLatest()
  } catch {
    return undefined
  }
}

async function waitForConfirmation(delayMs: number): Promise<void> {
  if (delayMs === 0) return
  await new Promise((resolve) => globalThis.setTimeout(resolve, delayMs))
}

function rejectUnconfirmedWrite(error: unknown): never {
  notifyErrorOnce(error, '保存结果无法确认，请检查网络后重试')
  throw error
}

function successfulWrite<T>(data: T, expectedVersion: number): VersionedWriteResult<T> {
  return { data, recovered: false, version: expectedVersion + 1 }
}

function planConfigMatches(draft: DraftOrderVO, originalUuid: string, plan: unknown): boolean {
  const config = draft.configs?.find((item) => item.originalUuid === originalUuid)
  return config?.configType === 'singlePlan'
    && config.preview !== undefined
    && matchesExpected(config.plan, plan)
}

function routeConfigMatches(draft: DraftOrderVO, route: ProcessRoutePreviewDTO): boolean {
  const config = draft.configs?.find((item) => item.originalUuid === route.originalUuid)
  return config?.configType === 'routePlan'
    && config.routePreview !== undefined
    && matchesExpected(config.route?.stages, route.stages)
}

function optionalValueMatches(actual: unknown, expected: unknown): boolean {
  if (expected === undefined || expected === null || expected === '') {
    return actual === undefined || actual === null || actual === ''
  }
  return actual === expected
}

function matchesExpected(actual: unknown, expected: unknown): boolean {
  if (expected === undefined) return true
  if (expected === null || typeof expected !== 'object') return optionalValueMatches(actual, expected)
  if (Array.isArray(expected)) {
    return Array.isArray(actual) && actual.length === expected.length
      && expected.every((item, index) => matchesExpected(actual[index], item))
  }
  if (typeof actual !== 'object' || actual === null || Array.isArray(actual)) return false
  const actualRecord = actual as Record<string, unknown>
  return Object.entries(expected).every(([key, value]) => matchesExpected(actualRecord[key], value))
}
