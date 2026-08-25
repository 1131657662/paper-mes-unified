import type { FinishProductionVO, RollProductionVO } from '../../../types/processOrder'
import { roundWeightTotal } from '../../../utils/integerWeightAllocation'
import {
  effectiveSourceConsumptionRatios,
  validateExplicitSourceConsumptionRatios,
} from '../../../utils/sourceConsumptionRatios'
import { allocateFinishGroupWeights } from './canonicalFinishWeightAllocation'
import {
  finishGroupsBySource,
  finishGroupSourceWeight,
  finishGroupSourceWidth,
  finishGroupTargetWeight,
  finishGroupWidthPolicy,
} from './canonicalFinishSourceBudget'

export { canonicalStageOutputWeights } from './canonicalStageEstimateWeight'

export interface CanonicalEstimateOptions {
  finishes?: FinishProductionVO[]
  production: RollProductionVO
  sourceProductions?: RollProductionVO[]
}

export type CanonicalWeightMap = Map<string, number | undefined>

export function weightFromCanonicalMap(
  weights: CanonicalWeightMap,
  uuid: string,
  legacyWeight?: number,
): number | undefined {
  return weights.has(uuid) ? weights.get(uuid) : legacyWeight
}

/** Keeps a complete saved plan; incomplete legacy groups still close against the source budget. */
export function canonicalFinishEstimateWeights(
  options: CanonicalEstimateOptions,
): CanonicalWeightMap {
  const finishes = options.finishes ?? options.production.finishes ?? []
  const candidates = finishes.filter(isAllocatableFinish)
  if (candidates.length === 0) return fallbackFinishWeights(finishes)

  const sourceRelations = sourceScopeFinishes(options, finishes)
    .flatMap((finish) => finish.sources ?? [])
  validateExplicitSourceConsumptionRatios(sourceRelations)
  const effectiveRatios = effectiveSourceConsumptionRatios(sourceRelations)
  const result: CanonicalWeightMap = new Map()
  for (const group of finishGroupsBySource(candidates, options.production)) {
    if (hasCompleteStoredFinishPlan(group)) {
      group.forEach((finish) => result.set(finish.uuid, storedFinishWeight(finish)))
      continue
    }
    allocateCanonicalGroup(options, group, effectiveRatios, result)
  }
  return result
}

export function canonicalFinishEstimateWeight(
  finish: FinishProductionVO,
  options: CanonicalEstimateOptions,
): number | undefined {
  const weights = canonicalFinishEstimateWeights(options)
  return weightFromCanonicalMap(weights, finish.uuid, fallbackFinishWeight(finish))
}

function allocateCanonicalGroup(
  options: CanonicalEstimateOptions,
  finishes: FinishProductionVO[],
  effectiveRatios: ReturnType<typeof effectiveSourceConsumptionRatios>,
  result: CanonicalWeightMap,
): void {
  const budgetOptions = { ...options, finishes, effectiveRatios }
  const sourceWeight = finishGroupSourceWeight(budgetOptions)
  if (sourceWeight == null) {
    finishes.forEach((finish) => result.set(finish.uuid, undefined))
    return
  }
  if (sourceWeight <= 0) {
    finishes.forEach((finish) => result.set(finish.uuid, fallbackFinishWeight(finish)))
    return
  }
  allocateFinishGroupWeights({
    finishes,
    production: options.production,
    sourceWidth: finishGroupSourceWidth(budgetOptions),
    targetWeight: finishGroupTargetWeight(budgetOptions, sourceWeight),
    widthPolicy: finishGroupWidthPolicy(budgetOptions),
  }, result)
}

function sourceScopeFinishes(
  options: CanonicalEstimateOptions,
  finishes: FinishProductionVO[],
): FinishProductionVO[] {
  const byUuid = new Map<string, FinishProductionVO>()
  for (const production of [options.production, ...(options.sourceProductions ?? [])]) {
    for (const finish of production.finishes ?? []) byUuid.set(finish.uuid, finish)
  }
  for (const finish of finishes) byUuid.set(finish.uuid, finish)
  return Array.from(byUuid.values())
}

function isAllocatableFinish(finish: FinishProductionVO): boolean {
  return finish.rollNoStatus !== 3 && finish.isSpare !== 1
}

function fallbackFinishWeights(finishes: FinishProductionVO[]): CanonicalWeightMap {
  return new Map(finishes
    .map((finish) => [finish.uuid, fallbackFinishWeight(finish)] as const)
    .filter((entry): entry is readonly [string, number] => entry[1] != null))
}

function fallbackFinishWeight(finish: FinishProductionVO): number | undefined {
  return storedFinishWeight(finish)
}

function hasStoredFinishWeight(finish: FinishProductionVO): boolean {
  return storedFinishWeight(finish) != null
}

function hasCompleteStoredFinishPlan(finishes: FinishProductionVO[]): boolean {
  return finishes.length > 0 && finishes.every(hasStoredFinishWeight)
}

function storedFinishWeight(finish: FinishProductionVO): number | undefined {
  const value = finish.estimateWeightSnap ?? (finish.finishRollNo ? finish.estimateWeight : undefined)
  return value == null || !Number.isFinite(value) || value < 0 ? undefined : roundWeightTotal(value)
}
