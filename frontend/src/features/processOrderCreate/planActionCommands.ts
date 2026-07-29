import { message } from 'antd'
import type { Machine } from '../../types/machine'
import type {
  PlanPreviewVO,
  ProcessPlanBatchSaveDTO,
  ProcessPlanDTO,
  ProcessPlanPreviewRequestDTO,
} from '../../types/processOrder'
import { configKeysForRoll } from './configuredPlanStatus'
import { plansFromBatch, previewsFromBatch } from './createOrderState'
import type { DefaultPlanOptions } from './draftMappers'
import { PlanOperationTracker } from './planOperationTracker'
import { classifyPlanBatchResult, type PlanBatchSaveResult, type PlanSaveResult } from './planSaveResult'
import {
  applySavedPlans,
  notifyBatchSave,
  notifySingleSave,
  omitKeys,
  pickPreviews,
  type PlanCommandState,
} from './planActionResult'
import { prepareBatchPlan, prepareSingleRollPlan } from './prepareProcessPlan'
import { normalizeRewindPlan } from './rewindLayerPlanUtils'
import type { RollDraft } from './types'
import type { VersionedWriteResult } from './draftWriteTypes'

export interface PlanActionDependencies {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  previewPlan: (request: PreviewMutationRequest) => Promise<PlanPreviewVO>
  savePlan: (request: SaveMutationRequest) => Promise<VersionedWriteResult<PlanPreviewVO>>
  savePlanBatch: (request: BatchMutationRequest) => Promise<VersionedWriteResult<PlanPreviewVO[]>>
  state: PlanCommandState
  tracker: PlanOperationTracker
}

interface PreviewMutationRequest {
  orderUuid: string
  request: ProcessPlanPreviewRequestDTO
  signal?: AbortSignal
}

interface SaveMutationRequest extends PreviewMutationRequest {
  rollUuid: string
}

interface BatchMutationRequest {
  dto: ProcessPlanBatchSaveDTO
  orderUuid: string
}

export function createPlanActionCommands(deps: PlanActionDependencies) {
  return {
    changePlan: (localId: string, plan: ProcessPlanDTO) => changePlan(deps, localId, plan),
    previewPlan: (roll: RollDraft, plan: ProcessPlanDTO, signal?: AbortSignal) => (
      previewPlan(deps, roll, plan, signal)
    ),
    savePlan: (roll: RollDraft, plan: ProcessPlanDTO) => savePlan(deps, roll, plan),
    savePlanBatch: (rolls: RollDraft[], plan: ProcessPlanDTO) => savePlanBatch(deps, rolls, plan),
  }
}

function changePlan(deps: PlanActionDependencies, localId: string, plan: ProcessPlanDTO) {
  const roll = deps.state.rolls.find((item) => item.localId === localId)
  const nextPlan = roll ? normalizeRewindPlan(plan, roll) : plan
  const staleKeys = new Set(roll ? configKeysForRoll(roll) : [localId])
  deps.tracker.markEdited(localId)
  deps.state.setPlans((previous) => ({ ...previous, [localId]: nextPlan }))
  deps.state.setConfiguredPlanIds((previous) => previous.filter((id) => !staleKeys.has(id)))
  deps.state.setPreviews((previous) => omitKeys(previous, staleKeys))
}

async function previewPlan(
  deps: PlanActionDependencies,
  roll: RollDraft,
  plan: ProcessPlanDTO,
  signal?: AbortSignal,
) {
  if (!deps.state.orderUuid || !roll.uuid) return
  const token = deps.tracker.begin('preview', roll.localId)
  const nextPlan = preparePlan(deps, roll, plan)
  const preview = await deps.previewPlan({
    orderUuid: deps.state.orderUuid,
    signal,
    request: planRequest(deps, roll.uuid, nextPlan),
  })
  if (signal?.aborted || !deps.tracker.isCurrent(token)) return
  deps.state.setPlans((previous) => ({ ...previous, [roll.localId]: nextPlan }))
  deps.state.setPreviews((previous) => ({ ...previous, [roll.localId]: preview }))
}

async function savePlan(
  deps: PlanActionDependencies,
  roll: RollDraft,
  plan: ProcessPlanDTO,
): Promise<PlanSaveResult> {
  if (!deps.state.orderUuid || !roll.uuid) return false
  const token = deps.tracker.begin('save', roll.localId)
  deps.tracker.begin('preview', roll.localId)
  const nextPlan = preparePlan(deps, roll, plan)
  const result = await deps.savePlan({
    orderUuid: deps.state.orderUuid,
    rollUuid: roll.uuid,
    request: planRequest(deps, roll.uuid, nextPlan),
  })
  const preview = result.data
  deps.state.setDraftVersion(result.version)
  if (!deps.tracker.isCurrent(token)) {
    message.warning('保存期间检测到新的修改，已保留当前内容，请再次保存')
    return { applied: false, preview }
  }
  applySavedPlans(deps.state, [roll], { [roll.localId]: nextPlan }, { [roll.localId]: preview })
  notifySingleSave(preview)
  return { applied: true, preview }
}

async function savePlanBatch(
  deps: PlanActionDependencies,
  targetRolls: RollDraft[],
  plan: ProcessPlanDTO,
): Promise<PlanBatchSaveResult | false> {
  if (!deps.state.orderUuid) return false
  const rolls = targetRolls.filter((roll) => roll.uuid)
  const first = rolls[0]
  if (!first) {
    message.warning('请选择已保存的母卷')
    return false
  }
  const tokens = new Map(rolls.map((roll) => [roll.localId, deps.tracker.begin('save', roll.localId)]))
  rolls.forEach((roll) => deps.tracker.begin('preview', roll.localId))
  const batchPlan = prepareBatchPlan({ ...deps, plan, roll: first })
  const result = await deps.savePlanBatch({
    orderUuid: deps.state.orderUuid,
    dto: { expectedVersion: deps.state.getDraftVersion(), originalUuids: rollUuids(rolls), plan: batchPlan },
  })
  deps.state.setDraftVersion(result.version)
  return applyBatchResult(deps, rolls, batchPlan, result.data, tokens)
}

function applyBatchResult(
  deps: PlanActionDependencies,
  rolls: RollDraft[],
  plan: ProcessPlanDTO,
  response: PlanPreviewVO[],
  tokens: Map<string, ReturnType<PlanOperationTracker['begin']>>,
) {
  const previews = previewsFromBatch(rolls, response)
  const appliedRolls = rolls.filter((roll) => previews[roll.localId]
    && tokenIsCurrent(deps.tracker, tokens.get(roll.localId)))
  const appliedPreviews = pickPreviews(previews, appliedRolls)
  applySavedPlans(deps.state, appliedRolls, plansFromBatch(appliedRolls, plan), appliedPreviews)
  notifyBatchSave(rolls.length, appliedRolls.length, appliedPreviews)
  return classifyPlanBatchResult(rolls, appliedPreviews)
}

function planRequest(deps: PlanActionDependencies, originalUuid: string, plan: ProcessPlanDTO) {
  return { expectedVersion: deps.state.getDraftVersion(), originalUuid, plan }
}

function preparePlan(deps: PlanActionDependencies, roll: RollDraft, plan: ProcessPlanDTO) {
  return prepareSingleRollPlan({
    defaultPlanOptions: deps.defaultPlanOptions,
    machines: deps.machines,
    plan,
    roll,
  })
}

function tokenIsCurrent(
  tracker: PlanOperationTracker,
  token: ReturnType<PlanOperationTracker['begin']> | undefined,
) {
  return token ? tracker.isCurrent(token) : false
}

function rollUuids(rolls: RollDraft[]) {
  return rolls.flatMap((roll) => roll.uuid ? [roll.uuid] : [])
}
