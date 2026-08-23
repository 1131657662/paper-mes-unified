import type { RollProductionVO } from '../../types/processOrder'
import { roundWeightTotal } from '../../utils/integerWeightAllocation'

/** Returns the integer source weight used by planning when no stage output is selected. */
export function productionSourceEstimateWeight(production: RollProductionVO): number {
  if (isPositiveFinite(production.actualWeight)) {
    return roundWeightTotal(production.actualWeight)
  }
  if (production.weightStatus === 'UNKNOWN') return 0
  if (isPositiveFinite(production.totalWeight)) {
    return roundWeightTotal(production.totalWeight)
  }
  const value = Number(production.rollWeight ?? 0) * Number(production.pieceNum ?? 1)
  return Number.isFinite(value) && value > 0 ? roundWeightTotal(value) : 0
}

export function isProductionWeightKnown(production: RollProductionVO): boolean {
  if (isPositiveFinite(production.actualWeight)) return true
  if (production.weightStatus === 'UNKNOWN') return false
  if (isPositiveFinite(production.totalWeight)) return true
  const value = Number(production.rollWeight ?? 0) * Number(production.pieceNum ?? 1)
  return Number.isFinite(value) && value > 0
}

function isPositiveFinite(value?: number): value is number {
  return value != null && Number.isFinite(value) && value > 0
}
