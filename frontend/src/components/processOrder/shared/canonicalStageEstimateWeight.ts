import type { RollProductionVO, StageOutputVO } from '../../../types/processOrder'
import type { CanonicalWeightMap } from './canonicalEstimateWeight'
import {
  allocateStageWeights,
  setFallback,
  stageSourceBudget,
} from './canonicalStageWeightBudget'

/** Rebuilds every stage output from its preceding stage's closed budget. */
export function canonicalStageOutputWeights(
  production: RollProductionVO,
  outputs: StageOutputVO[],
): CanonicalWeightMap {
  const result: CanonicalWeightMap = new Map()
  const ordered = [...outputs].sort((left, right) => (
    (left.stageLevel ?? 1) - (right.stageLevel ?? 1)
      || (left.outputSort ?? 0) - (right.outputSort ?? 0)
  ))
  const levels = Array.from(new Set(ordered.map((output) => output.stageLevel ?? 1)))
    .sort((left, right) => left - right)
  for (const level of levels) {
    const stage = ordered.filter((output) => (output.stageLevel ?? 1) === level)
    const source = stageSourceBudget(production, stage, outputs, result)
    if (source.weight == null) {
      stage.forEach((output) => result.set(output.uuid, undefined))
    } else if (source.weight <= 0) {
      stage.forEach((output) => setFallback(result, output))
    } else {
      allocateStageWeights(source, stage, production, result)
    }
  }
  return result
}
