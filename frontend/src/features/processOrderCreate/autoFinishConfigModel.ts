import type { ProcessPlanDTO, ProcessRoutePreviewDTO } from '../../types/processOrder'
import { mergedSourceUuidSet } from './rewindConsumptionUtils'
import type { RollDraft } from './types'

interface PendingConfigOptions {
  configuredPlanIds: string[]
  plans: Record<string, ProcessPlanDTO>
  rolls: RollDraft[]
  routes: Record<string, ProcessRoutePreviewDTO>
}

export function pendingConfigurationRolls(options: PendingConfigOptions): RollDraft[] {
  const configured = new Set(options.configuredPlanIds)
  const mergedSources = mergedSourceUuidSet(options.rolls, options.plans)
  return options.rolls.filter((roll) => {
    if (roll.processMode === 3 || roll.processMode === 4 || configured.has(roll.localId)) return false
    if (roll.uuid && (options.routes[roll.uuid] || mergedSources.has(roll.uuid))) return false
    return true
  })
}
