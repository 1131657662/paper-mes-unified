import type {
  DraftOrderBaseDTO,
  DraftOrderVO,
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../types/processOrder'
import type { Machine } from '../../types/machine'
import {
  applyLegacyPlanPriceDefaults,
  baseInfoFromOrder,
  defaultPlanForRoll,
  newRollDraft,
  rollDraftFromOriginal,
} from './draftMappers'
import { normalizeRewindPlan } from './rewindLayerPlanUtils'
import type { RollDraft } from './types'
import { MAX_SOURCE_PIECES, processModeRequiresMain } from '../../constants/processOrder'
import { applyDefaultMachineToPlan } from './machineDefaults'

export interface HydratedCreateOrderState {
  orderUuid?: string
  baseInfo?: DraftOrderBaseDTO
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  routes: Record<string, ProcessRoutePreviewDTO>
  selectedId?: string
  current: number
  configuredPlanIds: string[]
}

export function hydrateDraftState(draft: DraftOrderVO): HydratedCreateOrderState {
  const rolls = (draft.rolls ?? []).map(rollDraftFromOriginal)
  const safeRolls = rolls.length ? rolls : [newRollDraft()]
  const plans: Record<string, ProcessPlanDTO> = {}
  const previews: Record<string, PlanPreviewVO> = {}
  const routes: Record<string, ProcessRoutePreviewDTO> = {}
  const routePreviews: Record<string, ProcessRoutePreviewVO> = {}
  const configuredPlanIds: string[] = []

  for (const config of draft.configs ?? []) {
    if (!config.originalUuid) continue
    if (config.configStatus === 1) configuredPlanIds.push(config.originalUuid)
    if (config.configType === 'routePlan' && config.route) {
      routes[config.originalUuid] = config.route
      if (config.routePreview) routePreviews[config.originalUuid] = config.routePreview
      continue
    }
    if (config.plan) plans[config.originalUuid] = config.plan
    if (config.preview) previews[config.originalUuid] = config.preview
  }

  for (const roll of safeRolls) {
    const plan = plans[roll.localId] ?? defaultPlanForRoll(roll)
    plans[roll.localId] = normalizeRewindPlan(plan, roll)
  }

  return {
    orderUuid: draft.order?.uuid,
    baseInfo: draft.order ? baseInfoFromOrder(draft.order as unknown as Record<string, unknown>) : undefined,
    rolls: safeRolls,
    plans,
    previews,
    routePreviews,
    routes,
    selectedId: safeRolls[0]?.localId,
    current: draft.currentStep ?? 0,
    configuredPlanIds,
  }
}

export function plansForRolls(
  rolls: RollDraft[],
  currentPlans: Record<string, ProcessPlanDTO>,
  options: PlansForRollsOptions = {},
  machines?: Machine[],
) {
  return rolls.reduce<Record<string, ProcessPlanDTO>>((next, roll) => {
    const existing = currentPlans[roll.localId]
    const plan = existing && planMatchesRoll(existing, roll)
      ? rebasePlanForRoll(applyLegacyPlanPriceDefaults(existing, options), roll)
      : defaultPlanForRoll(roll, options)
    next[roll.localId] = machines ? applyDefaultMachineToPlan(plan, machines, roll) : plan
    return next
  }, {})
}

export function rebasePlanForRoll(plan: ProcessPlanDTO, roll: RollDraft): ProcessPlanDTO {
  const next: ProcessPlanDTO = {
    ...plan,
    machineUuid: plan.machineUuid ?? roll.machineUuid,
    finishSpecs: plan.finishSpecs?.map((spec) => ({
      ...spec,
      layers: spec.layers?.map((layer) => ({ ...layer })),
      sources: spec.sources?.map((source) => ({ ...source })),
    })),
    segments: plan.segments?.map((segment) => ({
      ...segment,
      layoutItems: segment.layoutItems?.map((item) => ({
        ...item,
        layers: item.layers?.map((layer) => ({ ...layer })),
      })),
      sources: segment.sources?.map((source) => ({ ...source })),
    })),
  }

  if (next.rewindMode === 5) return next

  const singleSource = roll.uuid
    ? [{ originalUuid: roll.uuid, shareRatio: 100, consumeRatio: 100, sourceSort: 1 }]
    : []
  next.segments = next.segments?.map((segment) => ({ ...segment, sources: singleSource }))
  next.finishSpecs = next.finishSpecs?.map((spec) => ({
    ...spec,
    sources: roll.uuid ? [{ originalUuid: roll.uuid, shareRatio: 100, consumeRatio: 100 }] : undefined,
  }))
  return next
}

export function isRollReadyForSave(roll: RollDraft) {
  if (!roll.paperName.trim()) return false
  if (!positiveInteger(roll.gramWeight) || !positiveInteger(roll.originalWidth)) return false
  if (!positiveInteger(roll.pieceNum ?? 1) || (roll.pieceNum ?? 1) > MAX_SOURCE_PIECES) return false
  if (roll.weightStatus !== 'UNKNOWN' && !positiveNumber(roll.rollWeight)) return false
  return roll.processMode !== 3 || Boolean(roll.rollNo?.trim())
}

function positiveInteger(value?: number) {
  return Number.isInteger(value) && positiveNumber(value)
}

function positiveNumber(value?: number) {
  return value != null && Number.isFinite(value) && value > 0
}

export function plansFromBatch(rolls: RollDraft[], plan: ProcessPlanDTO) {
  return Object.fromEntries(rolls.map((roll) => [roll.localId, rebasePlanForRoll(plan, roll)]))
}

export function previewsFromBatch(rolls: RollDraft[], previews: PlanPreviewVO[]) {
  const byOriginalUuid = new Map(previews
    .filter((preview) => preview.originalUuid)
    .map((preview) => [preview.originalUuid!, preview]))
  return Object.fromEntries(rolls.flatMap((roll) => {
    const preview = roll.uuid ? byOriginalUuid.get(roll.uuid) : undefined
    return preview ? [[roll.localId, preview]] : []
  }))
}

export function planMatchesRoll(plan: ProcessPlanDTO | undefined, roll: RollDraft) {
  if (!plan) return false
  if (plan.processMode !== roll.processMode) return false
  if (!processModeRequiresMain(roll.processMode)) return true
  return plan.mainStepType === roll.mainStepType
    && plan.machineUuid === roll.machineUuid
}

interface PlansForRollsOptions {
  spareCount?: number
  sawPrice?: number
  rewindPrice?: number
}
