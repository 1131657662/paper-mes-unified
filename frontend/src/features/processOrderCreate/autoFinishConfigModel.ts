import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO } from '../../types/processOrder'
import { isConfiguredPlanReady } from './configuredPlanStatus'
import { mergedSourceUuidSet } from './rewindConsumptionUtils'
import type { RollDraft } from './types'

interface PendingConfigOptions {
  configuredPlanIds: string[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  rolls: RollDraft[]
}

export function pendingConfigurationRolls(options: PendingConfigOptions): RollDraft[] {
  const configured = new Set(options.configuredPlanIds)
  const mergedSources = mergedSourceUuidSet(options.rolls, options.plans)
  return options.rolls.filter((roll) => {
    if (roll.processMode === 3 || roll.processMode === 4) return false
    if (roll.uuid && (options.routePreviews[roll.uuid] || mergedSources.has(roll.uuid))) return false
    return !isConfiguredPlanReady(roll, configured, options.previews)
  })
}
