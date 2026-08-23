import { STEP_TYPE } from '../../../constants/processOrder'
import { isVisibleProductionOutput } from '../../../components/processOrder/shared/detailHelpers'
import { canonicalStageOutputWeights, weightFromCanonicalMap } from '../../../components/processOrder/shared/canonicalEstimateWeight'
import { sortFinishOutputs } from '../../../components/processOrder/shared/outputOrder'
import type {
  FinishRoll,
  ProcessStep,
  RollProductionVO,
  StageOutputVO,
} from '../../../types/processOrder'
import { formatOptionalTon as formatRawTon } from '../../../utils/numberFormatters'
import { formatTon } from '../orderDetailUtils'
import { layeredRouteOutputs } from './printPreviewLayeredOutputs'
import {
  outputsWithTrim,
  routeOutput,
  routeOutputEstimateWeights,
  singleStageOutputs,
} from './printPreviewOutputs'
import { singleStageRequirement, stageRequirement } from './printPreviewRequirements'
import type { PrintRouteStage } from './printPreviewTypes'
import { directShipStage } from './printPreviewDirectShip'

export function buildPrintRouteStages(
  production: RollProductionVO,
  detailSteps: ProcessStep[],
  finishRolls: FinishRoll[] = [],
): PrintRouteStage[] {
  if (production.processMode === 3) return [directShipStage(production, finishRolls)]
  const steps = mergedSteps(production, detailSteps)
  const outputs = activeStageOutputs(production.stageOutputs)
  return outputs.length
    ? routeStagesFromOutputs(production, outputs, steps)
    : routeStagesFromFinishes(production, steps)
}

function routeStagesFromOutputs(
  production: RollProductionVO,
  outputs: StageOutputVO[],
  steps: ProcessStep[],
): PrintRouteStage[] {
  const entries = routeLevels(steps, outputs).map((level, index) => {
    const levelOutputs = outputs.filter((item) => (item.stageLevel ?? 1) === level)
    const step = stageStep(steps, level, index, levelOutputs)
    const stageOutputsWithTrim = outputsWithTrim(production, step, levelOutputs, outputs)
    return { level, levelOutputs, step, stageOutputsWithTrim }
  })
  const estimateWeights = routeOutputEstimateWeights(
    production,
    entries.flatMap((entry) => entry.stageOutputsWithTrim),
  )
  return entries.map(({ level, levelOutputs, step, stageOutputsWithTrim }) => {
    return {
      key: `${production.originalUuid ?? 'roll'}-${level}`,
      stepType: step?.stepType ?? levelOutputs[0]?.sourceStepType,
      title: stageTitle(level, step, levelOutputs),
      source: sourceText(step, outputs, levelOutputs),
      metric: stepMetric(step, levelOutputs, outputs, production),
      requirement: stageRequirement(production, step, levelOutputs, outputs),
      outputs: layeredRouteOutputs(
        production,
        stageOutputsWithTrim.map((output) => routeOutput(
          output,
          production,
          estimateWeights.get(output.uuid),
        )),
        stageOutputsWithTrim,
        step,
      ),
    }
  })
}

function routeStagesFromFinishes(
  production: RollProductionVO,
  steps: ProcessStep[],
): PrintRouteStage[] {
  const orderedSteps = sortedSteps(steps)
  const step = orderedSteps[0]
  const outputs = sortFinishOutputs(
    production,
    (production.finishes ?? []).filter(isVisibleProductionOutput),
  )
  if (production.processMode === 4 && orderedSteps.length > 0) {
    return orderedSteps.map((serviceStep, index) => ({
      key: `${production.originalUuid ?? 'roll'}-service-${index + 1}`,
      stepType: serviceStep.stepType,
      title: `第${index + 1}道 ${serviceStep.stepName || STEP_TYPE[serviceStep.stepType ?? 0] || '附加工艺'}`,
      source: index === 0 ? '原卷' : '上一道服务工序',
      metric: stepMetric(serviceStep, [], [], production),
      requirement: singleStageRequirement(production, outputs, serviceStep),
      outputs: index === orderedSteps.length - 1
        ? singleStageOutputs(production, outputs, serviceStep)
        : [],
    }))
  }
  const fallbackTitle = production.processMode === 4
    ? '附加工艺'
    : STEP_TYPE[production.mainStepType ?? step?.stepType ?? 1]
  return [{
    key: `${production.originalUuid ?? 'roll'}-single`,
    stepType: step?.stepType ?? production.mainStepType,
    title: `第1道 ${step?.stepName || fallbackTitle || '加工'}`,
    source: '原卷',
    metric: stepMetric(step, [], [], production),
    requirement: singleStageRequirement(production, outputs, step),
    outputs: singleStageOutputs(production, outputs, step),
  }]
}

function activeStageOutputs(outputs?: StageOutputVO[]) {
  return (outputs ?? [])
    .filter((item) => item.outputStatus !== 4)
    .sort((a, b) => (
      (a.stageLevel ?? 0) - (b.stageLevel ?? 0)
      || (a.outputSort ?? 0) - (b.outputSort ?? 0)
    ))
}

function sortedSteps(steps?: ProcessStep[]) {
  return [...(steps ?? [])].sort((a, b) => (a.stepSort ?? 0) - (b.stepSort ?? 0))
}

function mergedSteps(production: RollProductionVO, detailSteps: ProcessStep[]) {
  const map = new Map<string, ProcessStep>()
  for (const step of production.steps ?? []) map.set(step.uuid, step)
  for (const step of detailSteps) {
    if (step.originalUuid === production.originalUuid) map.set(step.uuid, step)
  }
  return sortedSteps(Array.from(map.values()))
}

function routeLevels(steps: ProcessStep[], outputs: StageOutputVO[]) {
  const levels = new Set<number>()
  if (outputs.length) {
    for (const step of steps) {
      if (step.stageLevel != null) levels.add(step.stageLevel)
    }
    for (const output of outputs) levels.add(output.stageLevel ?? 1)
    return Array.from(levels).sort((a, b) => a - b)
  }
  steps.forEach((step, index) => levels.add(step.stageLevel ?? step.stepSort ?? index + 1))
  return Array.from(levels).sort((a, b) => a - b)
}

function stageStep(
  steps: ProcessStep[],
  level: number,
  index: number,
  outputs: StageOutputVO[],
) {
  return steps.find((item) => item.stageLevel === level)
    ?? steps[index]
    ?? steps.find((item) => item.stepType === outputs[0]?.sourceStepType)
}

function stageTitle(level: number, step?: ProcessStep, outputs: StageOutputVO[] = []) {
  const type = step?.stepType ?? outputs[0]?.sourceStepType
  return `第${level}道 ${step?.stepName || STEP_TYPE[type ?? 1] || '加工'}`
}

function sourceText(
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  stageOutputs: StageOutputVO[],
) {
  const inputUuid = step?.inputOutputUuid
    || stageOutputs.find((item) => item.parentOutputUuid)?.parentOutputUuid
  if (!inputUuid) return '原卷'
  const source = outputs.find((item) => item.uuid === inputUuid)
  return source?.outputNo ? `产物 ${source.outputNo}` : '上一阶段产物'
}

function stepMetric(
  step: ProcessStep | undefined,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
  production?: RollProductionVO,
) {
  if (!step) return fallbackMetric(stageOutputs, allOutputs, production)
  if (step.stepType === 1) return `刀数 ${step.knifeCount ?? 0} 刀`
  if (step.stepType === 2) return `复卷 ${formatRawTon(step.processWeight)}`
  if (step.stepType === 3 || step.stepType === 4) return serviceMetric(step)
  return '-'
}

function serviceMetric(step: ProcessStep) {
  if (step.billingMode === 4) return '免收服务费'
  if (step.billingBasis === 'PIECE') return `服务 ${step.serviceQuantity ?? '-'} 件`
  if (step.billingBasis === 'TON') return `服务 ${step.serviceQuantity ?? '-'} 吨`
  if (step.billingBasis === 'FIXED') return '按固定金额计价'
  return '服务数量待核定'
}

function fallbackMetric(
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
  production?: RollProductionVO,
) {
  const stepType = stageOutputs[0]?.sourceStepType
  if (stepType === 1) return '刀数 -'
  if (stepType !== 2) return '-'
  const parentWeight = sumParentWeights(stageOutputs, allOutputs, production)
  return parentWeight > 0 ? `复卷 ${formatTon(parentWeight)}` : '复卷 -'
}

function sumParentWeights(
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
  production?: RollProductionVO,
) {
  const declaredInputs = stageOutputs.flatMap((item) => item.inputOutputUuids?.filter(Boolean) ?? [])
  const parentUuids = new Set(declaredInputs.length > 0
    ? declaredInputs
    : stageOutputs.map((item) => item.parentOutputUuid).filter(Boolean))
  const estimates = production ? canonicalStageOutputWeights(production, allOutputs) : new Map<string, number>()
  return allOutputs.reduce((sum, output) => (
    parentUuids.has(output.uuid)
      ? sum + (output.actualWeight ?? weightFromCanonicalMap(estimates, output.uuid, output.estimateWeight) ?? 0)
      : sum
  ), 0)
}
