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

interface ProgressOptions {
  configuredPlanIds: string[]
  lockedRolls: Record<string, MergedSourceLock>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  rolls: RollDraft[]
  serviceConfigured: Record<string, boolean>
}

export function configStepProgress(options: ProgressOptions): ConfigStepProgress {
  const configured = new Set(options.configuredPlanIds)
  const progress = { noConfigCount: 0, pendingCount: 0, savedCount: 0, totalCount: options.rolls.length }
  for (const roll of options.rolls) incrementProgress(progress, roll, options, configured)
  return progress
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
  options: ProgressOptions,
  configured: Set<string>,
) {
  if (options.lockedRolls[roll.localId] || roll.processMode === 3) {
    progress.noConfigCount += 1
    return
  }
  if (roll.uuid && options.routePreviews[roll.uuid]) {
    progress.savedCount += 1
    return
  }
  if (isSaved(roll, options, configured)) progress.savedCount += 1
  else progress.pendingCount += 1
}

function isSaved(
  roll: RollDraft,
  options: ProgressOptions,
  configured: Set<string>,
) {
  if (roll.processMode === 4) return Boolean(roll.uuid && options.serviceConfigured[roll.uuid])
  return isConfiguredPlanReady(roll, configured, options.previews)
}
