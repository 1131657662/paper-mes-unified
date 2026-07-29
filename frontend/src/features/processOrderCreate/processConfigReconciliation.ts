import type { Machine } from '../../types/machine'
import type {
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../types/processOrder'
import { planMatchesRoll, plansForRolls } from './createOrderState'
import { configKeysForRoll } from './configuredPlanStatus'
import type { DefaultPlanOptions } from './draftMappers'
import type { RollDraft } from './types'

interface ProcessConfigSnapshot {
  configuredPlanIds: string[]
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  routes: Record<string, ProcessRoutePreviewDTO>
}

interface ReconcileOptions extends ProcessConfigSnapshot {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  rolls: RollDraft[]
}

export function reconcileProcessConfigAfterModeChange(options: ReconcileOptions): ProcessConfigSnapshot {
  const changedLocalIds = new Set(options.rolls
    .filter((roll) => !planMatchesRoll(options.plans[roll.localId], roll))
    .map((roll) => roll.localId))
  const changedUuids = new Set(options.rolls
    .filter((roll) => changedLocalIds.has(roll.localId) && roll.uuid)
    .map((roll) => roll.uuid!))
  const changedPlanKeys = new Set(options.rolls
    .filter((roll) => changedLocalIds.has(roll.localId))
    .flatMap(configKeysForRoll))

  return {
    configuredPlanIds: options.configuredPlanIds.filter((id) => !changedPlanKeys.has(id)),
    plans: plansForRolls(options.rolls, options.plans, options.defaultPlanOptions, options.machines),
    previews: omitKeys(options.previews, changedPlanKeys),
    routePreviews: omitKeys(options.routePreviews, changedUuids),
    routes: omitKeys(options.routes, changedUuids),
  }
}

function omitKeys<T>(record: Record<string, T>, omitted: Set<string>): Record<string, T> {
  return Object.fromEntries(Object.entries(record).filter(([key]) => !omitted.has(key)))
}
