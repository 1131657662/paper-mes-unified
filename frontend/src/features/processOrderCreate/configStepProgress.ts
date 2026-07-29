import type { MergedSourceLock } from './rewindConsumptionUtils'
import type { PlanPreviewVO, ProcessRoutePreviewVO } from '../../types/processOrder'
import { isConfiguredPlanReady } from './configuredPlanStatus'
import type { RollDraft } from './types'

export interface ConfigStepProgress {
  noConfigCount: number
  pendingCount: number
  savedCount: number
  totalCount: number
}

export interface ConfigStepProgressOptions {
  configuredPlanIds: string[]
  lockedRolls: Record<string, MergedSourceLock>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  rolls: RollDraft[]
  serviceConfigured: Record<string, boolean>
}

export function configStepProgress(options: ConfigStepProgressOptions): ConfigStepProgress {
  const configured = new Set(options.configuredPlanIds)
  const progress = { noConfigCount: 0, pendingCount: 0, savedCount: 0, totalCount: options.rolls.length }
  for (const roll of options.rolls) incrementProgress(progress, roll, options, configured)
  return progress
}

export function nextPendingConfigRoll(
  options: ConfigStepProgressOptions,
  currentLocalId: string,
  assumedSavedIds: Iterable<string> = [],
): RollDraft | undefined {
  const configured = new Set(options.configuredPlanIds)
  const assumedSaved = new Set(assumedSavedIds)
  const currentIndex = options.rolls.findIndex((roll) => roll.localId === currentLocalId)
  const ordered = currentIndex < 0
    ? options.rolls
    : [...options.rolls.slice(currentIndex + 1), ...options.rolls.slice(0, currentIndex + 1)]
  return ordered.find((roll) => !assumedSaved.has(roll.localId)
    && !isNoConfigRequired(roll, options)
    && !hasSavedRoute(roll, options)
    && !isSaved(roll, options, configured))
}

export function configStepProgressText(progress: ConfigStepProgress): string {
  return [
    `共 ${progress.totalCount} 卷`,
    `已保存 ${progress.savedCount}`,
    `无需配置 ${progress.noConfigCount}`,
    `待处理 ${progress.pendingCount}`,
  ].join(' · ')
}

function incrementProgress(
  progress: ConfigStepProgress,
  roll: RollDraft,
  options: ConfigStepProgressOptions,
  configured: Set<string>,
) {
  if (isNoConfigRequired(roll, options)) {
    progress.noConfigCount += 1
    return
  }
  if (hasSavedRoute(roll, options)) {
    progress.savedCount += 1
    return
  }
  if (isSaved(roll, options, configured)) progress.savedCount += 1
  else progress.pendingCount += 1
}

function isSaved(
  roll: RollDraft,
  options: ConfigStepProgressOptions,
  configured: Set<string>,
) {
  if (roll.processMode === 4) return Boolean(roll.uuid && options.serviceConfigured[roll.uuid])
  return isConfiguredPlanReady(roll, configured, options.previews)
}

function isNoConfigRequired(roll: RollDraft, options: ConfigStepProgressOptions) {
  return Boolean(options.lockedRolls[roll.localId]) || roll.processMode === 3
}

function hasSavedRoute(roll: RollDraft, options: ConfigStepProgressOptions) {
  return Boolean(roll.uuid && options.routePreviews[roll.uuid])
}
