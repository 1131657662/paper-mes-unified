import type { PlanPreviewVO } from '../../types/processOrder'
import type { RollDraft } from './types'

interface SavedPlanResult {
  localId: string
  preview?: PlanPreviewVO
}

export function reconcileConfiguredPlanIds(
  previousIds: string[],
  results: SavedPlanResult[],
): string[] {
  const nextIds = new Set(previousIds)
  for (const result of results) {
    if (result.preview?.ready === true) nextIds.add(result.localId)
    else nextIds.delete(result.localId)
  }
  return [...nextIds]
}

export function isConfiguredPlanReady(
  roll: RollDraft,
  configuredPlanIds: Iterable<string>,
  previews: Record<string, PlanPreviewVO>,
): boolean {
  const configured = configuredPlanIds instanceof Set
    ? configuredPlanIds
    : new Set(configuredPlanIds)
  const hasSavedId = configured.has(roll.localId)
    || Boolean(roll.uuid && configured.has(roll.uuid))
  return hasSavedId && previewForRoll(roll, previews)?.ready === true
}

export function previewForRoll(
  roll: RollDraft,
  previews: Record<string, PlanPreviewVO>,
): PlanPreviewVO | undefined {
  return previews[roll.localId] ?? (roll.uuid ? previews[roll.uuid] : undefined)
}
