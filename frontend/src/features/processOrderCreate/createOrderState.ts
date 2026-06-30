import type {
  DraftOrderBaseDTO,
  DraftOrderVO,
  PlanPreviewVO,
  ProcessPlanDTO,
} from '../../types/processOrder'
import {
  baseInfoFromOrder,
  defaultPlanForRoll,
  newRollDraft,
  rollDraftFromOriginal,
} from './draftMappers'
import type { RollDraft } from './types'

export interface HydratedCreateOrderState {
  orderUuid?: string
  baseInfo?: DraftOrderBaseDTO
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  selectedId?: string
  current: number
}

export function hydrateDraftState(draft: DraftOrderVO): HydratedCreateOrderState {
  const rolls = (draft.rolls ?? []).map(rollDraftFromOriginal)
  const safeRolls = rolls.length ? rolls : [newRollDraft()]
  const plans: Record<string, ProcessPlanDTO> = {}
  const previews: Record<string, PlanPreviewVO> = {}

  for (const config of draft.configs ?? []) {
    if (!config.originalUuid) continue
    if (config.plan) plans[config.originalUuid] = config.plan
    if (config.preview) previews[config.originalUuid] = config.preview
  }

  for (const roll of safeRolls) {
    plans[roll.localId] = plans[roll.localId] ?? defaultPlanForRoll(roll)
  }

  return {
    orderUuid: draft.order?.uuid,
    baseInfo: draft.order ? baseInfoFromOrder(draft.order as unknown as Record<string, unknown>) : undefined,
    rolls: safeRolls,
    plans,
    previews,
    selectedId: safeRolls[0]?.localId,
    current: draft.currentStep ?? 0,
  }
}

export function plansForRolls(
  rolls: RollDraft[],
  currentPlans: Record<string, ProcessPlanDTO>,
  options: PlansForRollsOptions = {},
) {
  return rolls.reduce<Record<string, ProcessPlanDTO>>((next, roll) => {
    const existing = currentPlans[roll.localId]
    next[roll.localId] = planMatchesRoll(existing, roll)
      ? rebasePlanForRoll(existing, roll)
      : defaultPlanForRoll(roll, { spareCount: options.spareCount })
    return next
  }, {})
}

export function rebasePlanForRoll(plan: ProcessPlanDTO, roll: RollDraft): ProcessPlanDTO {
  const next: ProcessPlanDTO = {
    ...plan,
    finishSpecs: plan.finishSpecs?.map((spec) => ({
      ...spec,
      layers: spec.layers?.map((layer) => ({ ...layer })),
      sources: spec.sources?.map((source) => ({ ...source })),
    })),
    segments: plan.segments?.map((segment) => ({
      ...segment,
      layoutItems: segment.layoutItems?.map((item) => ({ ...item })),
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
  return Boolean(roll.paperName && Number(roll.rollWeight ?? 0) > 0)
}

export function plansFromBatch(rolls: RollDraft[], plan: ProcessPlanDTO) {
  return Object.fromEntries(rolls.map((roll) => [roll.localId, rebasePlanForRoll(plan, roll)]))
}

export function previewsFromBatch(rolls: RollDraft[], previews: PlanPreviewVO[]) {
  return Object.fromEntries(rolls.map((roll, index) => [roll.localId, previews[index]]))
}

function planMatchesRoll(plan: ProcessPlanDTO | undefined, roll: RollDraft) {
  if (!plan) return false
  if (plan.processMode !== roll.processMode) return false
  if (roll.processMode === 3) return true
  return plan.mainStepType === roll.mainStepType
}

interface PlansForRollsOptions {
  spareCount?: number
}
