import {
  calcTrimWidth,
  isRemainProductionFinish,
  isVisibleProductionOutput,
  trimWeightFromFinishes,
} from '../../components/processOrder/shared/detailHelpers'
import type { FinishProductionVO, RollProductionVO, StageOutputVO } from '../../types/processOrder'
import { roundWeightTotal } from '../../utils/integerWeightAllocation'
import { productionSourceEstimateWeight } from './productionSourceWeight'
import { canonicalStageOutputWeights, weightFromCanonicalMap } from '../../components/processOrder/shared/canonicalEstimateWeight'

const OUTPUT_VOID = 4

export function activeFinishesWithFallbackTrim(
  finishes: FinishProductionVO[],
  production?: RollProductionVO,
): FinishProductionVO[] {
  const rows = finishes.filter(isVisibleProductionOutput)
  if (!production || rows.some(isRemainProductionFinish)) return rows
  const trimWidth = calcTrimWidth(production)
  const trimWeight = trimWeightFromFinishes(production.finishes, production)
  if (trimWidth <= 0 && trimWeight <= 0) return rows
  return [...rows, {
    uuid: `${production.originalUuid ?? 'roll'}-trim`,
    finishRollNo: '修边',
    isRemain: 1,
    paperName: production.paperName,
    gramWeight: production.gramWeight,
    finishWidth: trimWidth || undefined,
    estimateWeight: trimWeight || undefined,
  }]
}

export function stageOutputsWithFallbackTrim(outputs: StageOutputVO[], production?: RollProductionVO): StageOutputVO[] {
  const rows = outputs.filter((output) => output.outputStatus !== OUTPUT_VOID)
  if (!production || !rows.length) return rows
  const result = [...rows]
  for (const level of trimmableOutputLevels(rows)) {
    const levelOutputs = result.filter((output) => (output.stageLevel ?? 1) === level)
    const trim = fallbackStageTrimOutput(production, level, levelOutputs, result)
    if (trim) result.push(trim)
  }
  return result
}

export function isStageTrimOutput(output: StageOutputVO): boolean {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}

function trimmableOutputLevels(outputs: StageOutputVO[]): number[] {
  const levels = new Set<number>()
  for (const output of outputs) {
    if (output.sourceStepType === 1) levels.add(output.stageLevel ?? 1)
  }
  return Array.from(levels).sort((a, b) => a - b)
}

function fallbackStageTrimOutput(
  production: RollProductionVO,
  level: number,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): StageOutputVO | null {
  if (stageOutputs.some(isStageTrimOutput)) return null
  const source = stageSource(production, stageOutputs, allOutputs)
  const usedWidth = stageOutputs
    .filter((output) => !isStageTrimOutput(output))
    .reduce((sum, output) => sum + (output.finishWidth ?? 0), 0)
  if (!source.width || usedWidth <= 0) return null
  const trimWidth = source.width - usedWidth
  if (trimWidth <= 0) return null
  const stepType = stageOutputs[0]?.sourceStepType ?? 1
  return {
    uuid: `${production.originalUuid ?? 'roll'}-stage-${level}-trim`,
    outputNo: '修边',
    stageLevel: level,
    outputSort: 999,
    outputStatus: 0,
    outputType: 0,
    paperName: '修边/余料',
    gramWeight: production.gramWeight,
    finishWidth: trimWidth,
    estimateWeight: estimateTrimWeight(source.weight, source.width, trimWidth),
    sourceStepType: stepType,
    sourceSummary: stepTypeText(stepType),
  }
}

function stageSource(
  production: RollProductionVO,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): { width?: number; weight: number } {
  const declaredInputs = stageOutputs[0]?.inputOutputUuids?.filter(Boolean) ?? []
  const inputUuids = declaredInputs.length > 0
    ? declaredInputs
    : stageOutputs.map((output) => output.parentOutputUuid).filter((value): value is string => Boolean(value))
  const parents = allOutputs.filter((output) => inputUuids.includes(output.uuid))
  if (parents.length) {
    const estimates = canonicalStageOutputWeights(production, allOutputs)
    const widths = new Set(parents.map((parent) => parent.finishWidth).filter((width): width is number => width != null))
    return {
      width: widths.size === 1 ? Array.from(widths)[0] : undefined,
      weight: roundWeightTotal(parents.reduce(
        (sum, parent) => sum + (parent.actualWeight ?? weightFromCanonicalMap(estimates, parent.uuid, parent.estimateWeight) ?? 0),
        0,
      )),
    }
  }
  return {
    width: production.originalWidth,
    weight: productionSourceEstimateWeight(production),
  }
}

function estimateTrimWeight(sourceWeight: number, sourceWidth: number, trimWidth: number): number | undefined {
  if (sourceWeight <= 0 || sourceWidth <= 0 || trimWidth <= 0) return undefined
  return roundWeightTotal(sourceWeight * trimWidth / sourceWidth)
}

function stepTypeText(stepType?: number) {
  return stepType === 1 ? '锯纸' : stepType === 2 ? '复卷' : '加工'
}
