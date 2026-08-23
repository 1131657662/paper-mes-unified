import type { RollProductionVO, StageOutputVO } from '../../../types/processOrder'
import { allocateIntegerWeight, roundWeightTotal } from '../../../utils/integerWeightAllocation'
import type { CanonicalWeightMap } from './canonicalEstimateWeight'

export interface StageSourceBudget {
  weight?: number
  width?: number
}

export function allocateStageWeights(
  source: StageSourceBudget,
  stage: StageOutputVO[],
  production: RollProductionVO,
  result: CanonicalWeightMap,
) {
  const policy = stageWidthPolicy(production, stage)
  const stepType = stageStepType(production, stage)
  const step = stageStepFor(production, stage)
  const width = source.width
  const widthsKnown = stage.every((output) => positiveWidth(output.finishWidth) > 0)
  if (policy === 'LOSS'
    && step?.plannedLossWeight != null
    && step.plannedLossWeight > 0
    && (width == null || width <= 0 || !widthsKnown)) {
    allocateRemainingStageWeights(source.weight!, stage, stepType,
      roundWeightTotal(step.plannedLossWeight), result)
    return
  }
  if (!policy || width == null || width <= 0 || !widthsKnown) {
    allocateRemainingStageWeights(source.weight!, stage, stepType, 0, result)
    return
  }

  const trims = stage.filter(isStageTrimOutput)
  const products = stage.filter((output) => !isStageTrimOutput(output))
  const explicitTrimWidth = trims.reduce((sum, output) => sum + positiveWidth(output.finishWidth), 0)
  const productWidth = products.reduce((sum, output) => sum + positiveWidth(output.finishWidth), 0)
  const differenceWidth = Math.max(0, width - productWidth - explicitTrimWidth)
  const explicitTrimWeight = proportionalWeight(source.weight!, explicitTrimWidth, width)
  const differenceWeight = proportionalWeight(source.weight!, differenceWidth, width)
  const lossWeight = policy === 'LOSS'
    ? plannedOrProportionalLoss(production, stage, source.weight!, differenceWeight)
    : 0
  const rewindAllocation = policy === 'ALLOCATE' && stepType === 2 ? differenceWeight : 0
  const plannedTrimBudget = Math.min(
    source.weight!,
    policy === 'REMAINDER'
      ? proportionalWeight(source.weight!, explicitTrimWidth + differenceWidth, width)
      : explicitTrimWeight,
  )
  const trimsWithActual = trims.filter((output) => isPositiveFinite(output.actualWeight))
  const unknownTrims = trims.filter((output) => !trimsWithActual.includes(output))
  const measuredTrimWeight = trimsWithActual.reduce((sum, output) => sum + (output.actualWeight ?? 0), 0)
  const unknownTrimBudget = unknownTrims.length > 0
    ? Math.max(0, plannedTrimBudget - measuredTrimWeight) : 0
  const trimBudget = Math.min(source.weight!, measuredTrimWeight + unknownTrimBudget)
  const productsWithActual = products.filter((output) => isPositiveFinite(output.actualWeight))
  const unknownProducts = products.filter((output) => !productsWithActual.includes(output))
  const measuredProductWeight = productsWithActual.reduce((sum, output) => sum + (output.actualWeight ?? 0), 0)
  const finishBudget = source.weight! - trimBudget - lossWeight - rewindAllocation - measuredProductWeight
  const integerFinishBudget = wholeWeightBudget(finishBudget)
  const finishWeights = integerFinishBudget == null
    ? []
    : allocateIntegerWeight(integerFinishBudget,
      unknownProducts.map((output) => stageBasis(output, stepType)))
  const allocationWeights = rewindAllocation > 0
    ? allocateIntegerWeight(differenceWeight, unknownProducts.map(() => 1))
    : []
  unknownProducts.forEach((output, index) => {
    result.set(output.uuid, integerFinishBudget == null
      ? undefined : (finishWeights[index] ?? 0) + (allocationWeights[index] ?? 0))
  })
  const integerTrimBudget = wholeWeightBudget(unknownTrimBudget)
  if (integerTrimBudget == null) {
    unknownTrims.forEach((output) => result.set(output.uuid, undefined))
  } else {
    allocateIntegerWeight(integerTrimBudget, unknownTrims.map((output) => stageBasis(output, stepType)))
      .forEach((weight, index) => result.set(unknownTrims[index]!.uuid, weight))
  }
}

function allocateRemainingStageWeights(
  sourceWeight: number,
  stage: StageOutputVO[],
  stepType: number | undefined,
  lossWeight: number,
  result: CanonicalWeightMap,
) {
  const trims = stage.filter(isStageTrimOutput)
  const products = stage.filter((output) => !isStageTrimOutput(output))
  const measured = stage.filter(hasMeasuredWeight)
  const measuredWeight = measured.reduce((sum, output) => sum + (output.actualWeight ?? 0), 0)
  const knownTrimWeight = trims
    .filter((output) => !hasMeasuredWeight(output) && hasPositiveWeight(output.estimateWeight))
    .reduce((sum, output) => sum + (output.estimateWeight ?? 0), 0)
  const fixedWeight = measuredWeight + knownTrimWeight
  const target = sourceWeight - lossWeight - fixedWeight
  const unknownProducts = products.filter((output) => !hasMeasuredWeight(output))
  const integerTarget = wholeWeightBudget(target)
  if (integerTarget == null) {
    unknownProducts.forEach((output) => result.set(output.uuid, undefined))
  } else {
    allocateIntegerWeight(integerTarget, unknownProducts.map((output) => stageBasis(output, stepType)))
      .forEach((weight, index) => result.set(unknownProducts[index]!.uuid, weight))
  }
  trims.filter((output) => !hasMeasuredWeight(output) && !hasPositiveWeight(output.estimateWeight))
    .forEach((output) => result.set(output.uuid, undefined))
}

function hasMeasuredWeight(output: StageOutputVO) {
  return isPositiveFinite(output.actualWeight)
}

function hasPositiveWeight(value?: number) {
  return value != null && Number.isFinite(value) && value > 0
}

export function stageSourceBudget(
  production: RollProductionVO,
  stage: StageOutputVO[],
  allOutputs: StageOutputVO[],
  estimates: CanonicalWeightMap,
): StageSourceBudget {
  const declaredKeys = stage.flatMap(upstreamKeys)
  const stepInputKey = stageStepFor(production, stage)?.inputOutputUuid
  const parentKeys = Array.from(new Set(
    declaredKeys.length > 0 ? declaredKeys : stepInputKey ? [stepInputKey] : [],
  ))
  const parents = parentKeys.length > 0
    ? parentKeys.map((key) => allOutputs.find((output) => output.uuid === key))
    : []
  if (parents.length === 0 && (stage[0]?.stageLevel ?? 1) > 1) return {}
  if (parents.length > 0) {
    if (parents.some((parent) => !parent || isStageTrimOutput(parent))) return {}
    const knownParents = parents as StageOutputVO[]
    const parentWeights = knownParents.map((parent) => {
      if (isPositiveFinite(parent.actualWeight)) return parent.actualWeight
      if (parent.weightStatus === 'UNKNOWN') return undefined
      if (estimates.has(parent.uuid)) return estimates.get(parent.uuid)
      return isPositiveFinite(parent.estimateWeight)
        ? parent.estimateWeight : undefined
    })
    if (parentWeights.some((weight) => weight == null || weight <= 0)) return {}
    const parentWidths = knownParents.map((parent) => parent.finishWidth)
    const widths = new Set(parentWidths.filter((width): width is number => width != null && width > 0))
    return {
      weight: roundWeightTotal(parentWeights.reduce<number>((sum, weight) => sum + (weight ?? 0), 0)),
      width: parentWidths.every((width) => width != null && width > 0) && widths.size === 1
        ? Array.from(widths)[0] : undefined,
    }
  }
  return {
    weight: productionWeight(production),
    width: production.actualWidth != null && production.actualWidth > 0
      ? production.actualWidth : production.originalWidth,
  }
}

export function upstreamKeys(output: StageOutputVO): string[] {
  const inputKeys = output.inputOutputUuids?.filter(Boolean) ?? []
  if (inputKeys.length > 0) return inputKeys
  return output.parentOutputUuid ? [output.parentOutputUuid] : []
}

export function setFallback(result: CanonicalWeightMap, output: StageOutputVO) {
  const value = output.estimateWeight
  if (value != null && Number.isFinite(value) && value >= 0) result.set(output.uuid, roundWeightTotal(value))
}

function plannedOrProportionalLoss(
  production: RollProductionVO,
  stage: StageOutputVO[],
  sourceWeight: number,
  proportionalLoss: number,
) {
  const step = stageStepFor(production, stage)
  if (step?.plannedLossWeight != null && step.plannedLossWeight > 0) {
    return Math.min(sourceWeight, roundWeightTotal(step.plannedLossWeight))
  }
  return Math.min(sourceWeight, proportionalLoss)
}

function stageWidthPolicy(
  production: RollProductionVO,
  stage: StageOutputVO[],
): 'LOSS' | 'ALLOCATE' | 'REMAINDER' | undefined {
  const step = stageStepFor(production, stage)
  if (step?.widthDifferencePolicy) return step.widthDifferencePolicy
  const stepType = step?.stepType ?? stage[0]?.sourceStepType
  if (stepType === 1) return 'REMAINDER'
  if (stepType === 2) {
    const mode = production.rewindParams?.find((param) => param.paramMode != null)?.paramMode
    return mode === 2 || mode === 6 ? undefined : 'REMAINDER'
  }
  return undefined
}

function stageStepType(production: RollProductionVO, stage: StageOutputVO[]) {
  return stageStepFor(production, stage)?.stepType ?? stage[0]?.sourceStepType ?? production.mainStepType
}

function stageStepFor(production: RollProductionVO, stage: StageOutputVO[]) {
  const level = stage[0]?.stageLevel ?? 1
  const steps = [...(production.steps ?? [])].sort((left, right) => (
    (left.stepSort ?? 0) - (right.stepSort ?? 0)
  ))
  return steps.find((step) => step.stageLevel === level)
    ?? steps.find((step) => step.stepSort === level)
    ?? steps.find((step) => step.isMain === 1 && level === 1)
    ?? steps.find((step) => step.stepType === (stage[0]?.sourceStepType ?? production.mainStepType))
}

function proportionalWeight(total: number, width: number, sourceWidth: number) {
  if (total <= 0 || width <= 0 || sourceWidth <= 0) return 0
  return Math.min(total, roundWeightTotal(total * width / sourceWidth))
}

function positiveWidth(width?: number) {
  return width != null && Number.isFinite(width) && width > 0 ? width : 0
}

function isStageTrimOutput(output: StageOutputVO) {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}

function stageBasis(output: StageOutputVO, stepType?: number): number {
  if ((output.sourceStepType ?? stepType) === 1) return Math.max(1, positiveFinite(output.finishWidth) ?? 1)
  return Math.max(1, positiveFinite(output.estimateWeight) ?? positiveFinite(output.finishWidth) ?? 1)
}

function productionWeight(production: RollProductionVO): number | undefined {
  if (isPositiveFinite(production.actualWeight)) return roundWeightTotal(production.actualWeight)
  if (production.weightStatus === 'UNKNOWN') return undefined
  if (isPositiveFinite(production.totalWeight)) return roundWeightTotal(production.totalWeight)
  const value = Number(production.rollWeight ?? 0) * Math.max(1, production.pieceNum ?? 1)
  return Number.isFinite(value) && value > 0 ? roundWeightTotal(value) : undefined
}

function wholeWeightBudget(value: number): number | undefined {
  if (!Number.isFinite(value) || value < 0) return undefined
  const rounded = Math.round(value)
  return Math.abs(value - rounded) < 1e-9 ? rounded : undefined
}

function positiveFinite(value?: number): number | undefined {
  return value != null && Number.isFinite(value) && value > 0 ? value : undefined
}

function isPositiveFinite(value?: number): value is number {
  return positiveFinite(value) != null
}
