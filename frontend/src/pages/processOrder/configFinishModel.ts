import type {
  FinishConfigSaveDTO,
  OriginalRoll,
  RollProductionVO,
} from '../../types/processOrder'

export interface ConfigFinishSavePlan {
  skippedConfigured: string[]
  skippedSources: string[]
  toSave: OriginalRoll[]
}

export function buildDefaultConfig(roll: OriginalRoll): FinishConfigSaveDTO {
  const processMode = roll.processMode ?? 1
  if (roll.mainStepType === 1) return defaultSawConfig(processMode)
  if (roll.mainStepType === 2) return defaultRewindConfig(roll, processMode)
  return { processMode, mainStepType: roll.mainStepType, spareCount: 0, finishSpecs: [] }
}

export function configuredFinishCounts(productions: RollProductionVO[]): Map<string, number> {
  return new Map(productions.map((production) => [
    production.originalUuid ?? '',
    production.finishes?.length ?? 0,
  ]))
}

export function mergedSourceRollUuids(productions: RollProductionVO[]): Set<string> {
  const result = new Set<string>()
  for (const production of productions) {
    for (const finish of production.finishes ?? []) {
      for (const source of finish.sources ?? []) {
        if (source.originalUuid && source.originalUuid !== production.originalUuid) {
          result.add(source.originalUuid)
        }
      }
    }
  }
  return result
}

export function buildSavePlan(
  rolls: OriginalRoll[],
  finishCounts: Map<string, number>,
  sourceOnlyUuids: Set<string>,
): ConfigFinishSavePlan {
  const plan: ConfigFinishSavePlan = { skippedConfigured: [], skippedSources: [], toSave: [] }
  rolls.forEach((roll, index) => classifyRollForSave(roll, index, finishCounts, sourceOnlyUuids, plan))
  return plan
}

export function configForRoll(
  roll: OriginalRoll,
  configs: Record<string, FinishConfigSaveDTO>,
): FinishConfigSaveDTO {
  const config = configs[roll.uuid] ?? buildDefaultConfig(roll)
  return {
    ...config,
    processMode: roll.processMode ?? config.processMode,
    mainStepType: roll.mainStepType ?? config.mainStepType,
  }
}

function defaultSawConfig(processMode: number): FinishConfigSaveDTO {
  return {
    processMode,
    mainStepType: 1,
    knifeCount: 0,
    spareCount: 0,
    finishSpecs: [{ count: 1, finishWidth: processMode === 2 ? 0 : 400, estimateWeight: 0 }],
  }
}

function defaultRewindConfig(roll: OriginalRoll, processMode: number): FinishConfigSaveDTO {
  const width = roll.originalWidth ?? 500
  return {
    processMode,
    mainStepType: 2,
    rewindMode: 1,
    spareCount: 0,
    finishSpecs: [{ count: 1, finishWidth: processMode === 2 ? 0 : width, finishDiameter: 0, finishCoreDiameter: 3, estimateWeight: 0, splitRatio: 100 }],
    rewindSegments: [{
      segmentSort: 1,
      segmentRatio: 1,
      finishCoreDiameter: 3,
      repeatCount: 1,
      sources: [{ originalUuid: roll.uuid, shareRatio: 100 }],
      layoutItems: [{ width, quantity: 1, itemType: 'FINISH' }],
    }],
  }
}

function classifyRollForSave(
  roll: OriginalRoll,
  index: number,
  counts: Map<string, number>,
  sources: Set<string>,
  plan: ConfigFinishSavePlan,
) {
  if (roll.processMode === 3) return
  const label = `原纸${index + 1}`
  if ((counts.get(roll.uuid) ?? 0) > 0) return void plan.skippedConfigured.push(label)
  if (sources.has(roll.uuid)) return void plan.skippedSources.push(label)
  plan.toSave.push(roll)
}
