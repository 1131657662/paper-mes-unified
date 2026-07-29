import type { PlanPreviewVO } from '../../types/processOrder'
import type { RollDraft } from './types'

export interface PlanBatchSaveResult {
  appliedIds: string[]
  failedIds: string[]
  savedIds: string[]
}

export interface PlanSaveOutcome {
  applied: boolean
  preview: PlanPreviewVO
}

export type PlanSaveResult = PlanSaveOutcome | false

export function classifyPlanBatchResult(
  rolls: RollDraft[],
  previews: Record<string, PlanPreviewVO>,
): PlanBatchSaveResult {
  const failedIds = rolls
    .filter((roll) => previews[roll.localId]?.ready !== true)
    .map((roll) => roll.localId)
  const failed = new Set(failedIds)
  return {
    appliedIds: rolls.filter((roll) => previews[roll.localId]).map((roll) => roll.localId),
    failedIds,
    savedIds: rolls.filter((roll) => !failed.has(roll.localId)).map((roll) => roll.localId),
  }
}
