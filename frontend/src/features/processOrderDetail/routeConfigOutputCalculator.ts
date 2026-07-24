import type { ProcessPlanDTO } from '../../types/processOrder'
import {
  STEP_TYPE_SAW,
  routeStepName,
  type DetailRouteOutputRow,
} from './routeConfigModel'
import { calculateRewindOutputSeeds } from './routeConfigRewindCalculator'
import { calculateSawOutputSeeds } from './routeConfigSawCalculator'
import { combinedSource } from './routeConfigSource'

export function calculateRouteOutputs(
  stageLevel: number,
  sources: DetailRouteOutputRow[],
  plan: ProcessPlanDTO,
  existingRows: DetailRouteOutputRow[] = [],
): DetailRouteOutputRow[] {
  const source = combinedSource(sources)
  const seeds = plan.mainStepType === STEP_TYPE_SAW
    ? calculateSawOutputSeeds(source, plan)
    : calculateRewindOutputSeeds(sources, plan)
  const usedKeys = new Set(existingRows.map((row) => row.outputKey))
  return seeds.map((seed, index) => ({
    ...seed,
    label: seed.isRemain === 1
      ? `第${stageLevel}段修边`
      : `第${stageLevel}段产物 ${index + 1}`,
    outputKey: nextOutputKey(stageLevel, usedKeys, seed.isRemain === 1),
    parentOutputKey: source.outputKey,
    sourceStepType: plan.mainStepType,
    sourceOutputKey: source.outputKey,
    sourceSummary: routeStepName(plan.mainStepType),
    stageLevel,
  }))
}

function nextOutputKey(stageLevel: number, usedKeys: Set<string>, remain = false) {
  let sort = 1
  const prefix = remain ? 'T' : 'F'
  while (usedKeys.has(`S${stageLevel}-${prefix}${sort}`)) sort += 1
  const key = `S${stageLevel}-${prefix}${sort}`
  usedKeys.add(key)
  return key
}
