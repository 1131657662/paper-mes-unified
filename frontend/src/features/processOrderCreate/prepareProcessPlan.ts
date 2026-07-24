import type { Machine } from '../../types/machine'
import type { ProcessPlanDTO } from '../../types/processOrder'
import { rebasePlanForRoll } from './createOrderState'
import { applyLegacyPlanPriceDefaults, type DefaultPlanOptions } from './draftMappers'
import { applyDefaultMachineToPlan } from './machineDefaults'
import { normalizeLayeredRewindPlan } from './rewindLayerPlanUtils'
import type { RollDraft } from './types'

export interface PreparePlanOptions {
  defaultPlanOptions: DefaultPlanOptions
  machines: Machine[]
  plan: ProcessPlanDTO
  roll: RollDraft
}

export function prepareSingleRollPlan(options: PreparePlanOptions): ProcessPlanDTO {
  const pricedPlan = applyLegacyPlanPriceDefaults(options.plan, options.defaultPlanOptions)
  return normalizeLayeredRewindPlan(
    applyDefaultMachineToPlan(rebasePlanForRoll(pricedPlan, options.roll), options.machines),
    options.roll,
  )
}

export function prepareBatchPlan(options: PreparePlanOptions): ProcessPlanDTO {
  const pricedPlan = applyLegacyPlanPriceDefaults(options.plan, options.defaultPlanOptions)
  return normalizeLayeredRewindPlan(
    applyDefaultMachineToPlan(pricedPlan, options.machines),
    options.roll,
  )
}
