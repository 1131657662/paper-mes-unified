import {
  calcTrimWidth,
  isRemainProductionFinish,
  trimWeightFromFinishes,
} from '../../../components/processOrder/shared/detailHelpers'
import { buildFinishLayers } from '../../../components/processOrder/shared/layeredRewindView'
import {
  canonicalFinishEstimateWeights,
  canonicalStageOutputWeights,
  weightFromCanonicalMap,
} from '../../../components/processOrder/shared/canonicalEstimateWeight'
import { sortFinishOutputs } from '../../../components/processOrder/shared/outputOrder'
import type {
  FinishProductionVO,
  ProcessStep,
  RollProductionVO,
  StageOutputVO,
} from '../../../types/processOrder'
import { formatMm } from '../../../utils/numberFormatters'
import { roundWeightTotal } from '../../../utils/integerWeightAllocation'
import { formatProductionEstimateKg, formatProductionKg } from '../orderDetailUtils'
import { productionSourceEstimateWeight } from '../productionSourceWeight'
import { withPrintLayerTexts } from './printPreviewLayeredOutputs'
import {
  printFinishSpec,
  printOutputSpec,
  printTrimTitle,
} from './printPreviewSpecification'
import type { PrintRouteOutput } from './printPreviewTypes'

export function singleStageOutputs(
  production: RollProductionVO,
  outputs: FinishProductionVO[],
  step?: ProcessStep,
) {
  const sortedOutputs = sortFinishOutputs(production, outputs)
  const trim = fallbackSingleStageTrim(production, outputs, step)
  const estimateFinishes: FinishProductionVO[] = trim
    ? [...outputs, {
      uuid: trim.key,
      isRemain: 1,
      finishWidth: trim.width,
      estimateWeight: trim.weightValue,
    }]
    : outputs
  const estimates = canonicalFinishEstimateWeights({ production, finishes: estimateFinishes })
  const items = sortedOutputs.map((finish) => finishRouteOutput(
    finish,
    production,
    weightFromCanonicalMap(estimates, finish.uuid),
  ))
  const estimatedTrim = trim
    ? { ...trim, weightValue: weightFromCanonicalMap(estimates, trim.key, trim.weightValue) }
    : null
  return withPrintLayerTexts(
    estimatedTrim ? [...items, estimatedTrim] : items,
    buildFinishLayers(production, sortedOutputs),
  )
}

export function routeOutput(
  output: StageOutputVO,
  production: RollProductionVO,
  estimateWeight = output.estimateWeight,
) {
  const trim = isTrimOutput(output)
  return {
    key: output.uuid,
    finishRollUuid: output.finishRollUuid,
    name: trim ? printTrimTitle(output.outputNo) : output.outputNo || '-',
    spec: printOutputSpec(output),
    weight: formatProductionEstimateKg(estimateWeight),
    actualWeight: output.actualWeight == null
      ? undefined
      : formatProductionKg(output.actualWeight, production),
    weightValue: estimateWeight,
    width: output.finishWidth,
    status: trim ? 'trim' : isFinalOutput(output) ? 'final' : 'next',
  } satisfies PrintRouteOutput
}

export function routeOutputEstimateWeights(
  production: RollProductionVO,
  outputs: StageOutputVO[],
): Map<string, number | undefined> {
  return canonicalStageOutputWeights(production, outputs)
}

export function outputsWithTrim(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
) {
  const stepType = step?.stepType ?? stageOutputs[0]?.sourceStepType
  if (stepType !== 1 || stageOutputs.some(isTrimOutput)) return stageOutputs
  const source = stageSource(production, stageOutputs, allOutputs)
  const trim = trimOutput({
    key: `${production.originalUuid ?? 'roll'}-stage-${stageOutputs[0]?.stageLevel ?? 1}-trim`,
    production,
    sourceWeight: source.weight,
    sourceWidth: source.width,
    usedWidth: stageOutputs
      .filter((item) => !isTrimOutput(item))
      .reduce((sum, item) => sum + (item.finishWidth ?? 0), 0),
  })
  return trim ? [...stageOutputs, trimToStageOutput(trim, stageOutputs[0])] : stageOutputs
}

export function stageSource(
  production: RollProductionVO,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): { width?: number; weight: number } {
  const declaredInputs = stageOutputs[0]?.inputOutputUuids?.filter(Boolean) ?? []
  const inputUuids = declaredInputs.length > 0
    ? declaredInputs
    : stageOutputs.map((item) => item.parentOutputUuid).filter((value): value is string => Boolean(value))
  const parents = allOutputs.filter((item) => inputUuids.includes(item.uuid))
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

export function isFinalOutput(output: StageOutputVO) {
  return output.outputStatus === 3 || output.outputType === 2
}

export function isTrimOutput(output: StageOutputVO) {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}

function finishRouteOutput(
  finish: FinishProductionVO,
  production: RollProductionVO,
  estimateWeight = finish.estimateWeight,
): PrintRouteOutput {
  const remain = isRemainProductionFinish(finish)
  return {
    key: finish.uuid,
    finishRollUuid: finish.uuid,
    name: remain ? printTrimTitle(finish.finishRollNo) : finish.finishRollNo || '预生成成品',
    spec: printFinishSpec(finish),
    weight: formatProductionEstimateKg(estimateWeight),
    actualWeight: finish.actualWeight == null
      ? undefined
      : formatProductionKg(finish.actualWeight, production),
    weightValue: estimateWeight,
    width: finish.finishWidth,
    status: remain ? 'trim' : 'final',
  }
}

function fallbackSingleStageTrim(
  production: RollProductionVO,
  outputs: FinishProductionVO[],
  step?: ProcessStep,
): PrintRouteOutput | null {
  if (outputs.some(isRemainProductionFinish)) return null
  if ((step?.stepType ?? production.mainStepType) !== 1) return null
  const trimWidth = calcTrimWidth(production)
  const trimWeight = trimWeightFromFinishes(production.finishes, production)
  if (trimWidth <= 0 && trimWeight <= 0) return null
  const sourceWeight = productionSourceEstimateWeight(production)
  const estimateWeight = trimWeight > 0
    ? trimWeight
    : estimateTrimWeight(sourceWeight, production.originalWidth, trimWidth)
  return {
    key: `${production.originalUuid ?? 'roll'}-trim`,
    name: '修边',
    spec: trimWidth > 0 ? formatMm(trimWidth) : '-',
    weight: estimateWeight == null ? '-' : formatProductionEstimateKg(estimateWeight),
    weightValue: estimateWeight,
    width: trimWidth,
    status: 'trim',
  }
}

interface TrimOutputOptions {
  key: string
  production: RollProductionVO
  sourceWeight: number
  sourceWidth?: number
  usedWidth: number
}

function trimOutput(options: TrimOutputOptions): PrintRouteOutput | null {
  if (!options.sourceWidth || options.usedWidth <= 0) return null
  const trimWidth = options.sourceWidth - options.usedWidth
  if (trimWidth <= 0) return null
  const trimWeight = options.sourceWeight > 0
    ? roundWeightTotal(options.sourceWeight * trimWidth / options.sourceWidth)
    : undefined
  return {
    key: options.key,
    name: '修边',
    spec: formatMm(trimWidth),
    weight: trimWeight == null ? '-' : formatProductionEstimateKg(trimWeight),
    weightValue: trimWeight,
    status: 'trim',
  }
}

function estimateTrimWeight(sourceWeight: number, sourceWidth?: number, trimWidth?: number) {
  if (!sourceWidth || !trimWidth || sourceWeight <= 0) return undefined
  return roundWeightTotal(sourceWeight * trimWidth / sourceWidth)
}

function trimToStageOutput(trim: PrintRouteOutput, sample?: StageOutputVO): StageOutputVO {
  return {
    uuid: trim.key,
    outputNo: trim.name,
    stageLevel: sample?.stageLevel,
    outputSort: 999,
    outputType: 0,
    outputStatus: 0,
    paperName: '修边',
    isRemain: 1,
    finishWidth: Number(trim.spec.replace('mm', '')),
    estimateWeight: Number(trim.weight.replace(/kg|,/g, '')) || undefined,
    sourceStepType: sample?.sourceStepType,
    remark: '修边/余料',
  }
}
