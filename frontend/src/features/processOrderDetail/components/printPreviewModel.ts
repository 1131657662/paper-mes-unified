import { IS_INVOICE, ORDER_SETTLE_TYPE, PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import {
  calcTrimWidth,
  fmtDiameter,
  isDeliverableProductionFinish,
  isRemainProductionFinish,
  isVisibleProductionOutput,
  trimWeightFromFinishes,
} from '../../../components/processOrder/shared/detailHelpers'
import {
  buildFinishLayers,
  buildStageOutputLayers,
  layerItemMap,
  layersSummaryText,
  type LayeredRewindLayer,
} from '../../../components/processOrder/shared/layeredRewindView'
import type {
  FinishProductionVO,
  ProcessOrderDetailVO,
  ProcessStep,
  RollProductionVO,
  StageOutputVO,
} from '../../../types/processOrder'
import { formatOptionalTon as formatRawTon } from '../../../utils/numberFormatters'
import { buildDetailMetrics, formatKg, formatTon } from '../orderDetailUtils'

export interface PrintRouteOutput {
  key: string
  layerText?: string
  name: string
  spec: string
  weight: string
  width?: number
  status: 'next' | 'final' | 'trim'
}

export interface PrintRouteStage {
  key: string
  title: string
  source: string
  metric: string
  requirement: string
  outputs: PrintRouteOutput[]
}

export interface PrintRollBlock {
  key: string
  title: string
  sourceItems: Array<{ label: string; value: string }>
  remark?: string
  routeStages: PrintRouteStage[]
}

export interface PrintSummaryItem {
  label: string
  value: string
}

export function buildPrintRollBlocks(detail: ProcessOrderDetailVO): PrintRollBlock[] {
  return buildDisplayRows(detail.rollProductions ?? []).map((row) => {
    const production = row.mainProduction
    const steps = mergedSteps(production, detail.steps)
    const stageOutputs = activeStageOutputs(production.stageOutputs)
    return {
      key: row.key,
      title: rollTitle(row.seq, production, row.isMergeGroup),
      sourceItems: sourceItems(production),
      remark: production.remark,
      routeStages: stageOutputs.length
        ? routeStagesFromOutputs(production, stageOutputs, steps)
        : routeStagesFromFinishes(production, steps),
    }
  })
}

export function buildPrintSummary(detail: ProcessOrderDetailVO): PrintSummaryItem[] {
  const metrics = buildDetailMetrics(detail)
  const rewindWeight = (detail.steps ?? []).reduce((sum, step) => (
    step.stepType === 2 ? sum + (step.processWeight ?? 0) : sum
  ), 0)
  const finalCount = (detail.finishRolls ?? []).filter(isFinalFinishRoll).length
  return [
    { label: '原卷', value: `${metrics.rollCount} 卷 / ${formatTon(metrics.totalOriginalWeight)}` },
    { label: '最终成品', value: `${finalCount} 件 / ${formatTon(metrics.totalEstimateWeight)}` },
    { label: '锯纸刀数', value: `${detail.order.actualTotalKnife ?? sumKnifeCount(detail.steps)} 刀` },
    { label: '复卷吨位', value: formatRawTon(rewindWeight) },
    { label: '工序数', value: `${detail.steps?.length ?? 0} 道` },
    { label: '开票/结算', value: `${IS_INVOICE[detail.order.isInvoice ?? 2] ?? '-'} / ${ORDER_SETTLE_TYPE[detail.order.settleType ?? 1] ?? '-'}` },
  ]
}

function routeStagesFromOutputs(
  production: RollProductionVO,
  outputs: StageOutputVO[],
  steps: ProcessStep[],
): PrintRouteStage[] {
  return routeLevels(steps, outputs).map((level, index) => {
    const levelOutputs = outputs.filter((item) => (item.stageLevel ?? 1) === level)
    const step = stageStep(steps, level, index, levelOutputs)
    const stageOutputsWithTrim = outputsWithTrim(production, step, levelOutputs, outputs)
    return {
      key: `${production.originalUuid ?? 'roll'}-${level}`,
      title: stageTitle(level, step, levelOutputs),
      source: sourceText(step, outputs, levelOutputs),
      metric: stepMetric(step, levelOutputs, outputs),
      requirement: stageRequirement(production, step, levelOutputs, outputs),
      outputs: layeredRouteOutputs(production, stageOutputsWithTrim.map(routeOutput), stageOutputsWithTrim, step),
    }
  })
}

function routeStagesFromFinishes(production: RollProductionVO, steps: ProcessStep[]): PrintRouteStage[] {
  const step = sortedSteps(steps)[0]
  const outputs = (production.finishes ?? []).filter(isVisibleProductionOutput)
  return [{
    key: `${production.originalUuid ?? 'roll'}-single`,
    title: `第1道 ${step?.stepName || STEP_TYPE[production.mainStepType ?? step?.stepType ?? 1] || '加工'}`,
    source: '原卷',
    metric: stepMetric(step, [], []),
    requirement: singleStageRequirement(production, outputs, step),
    outputs: singleStageOutputs(production, outputs, step),
  }]
}

function singleStageOutputs(
  production: RollProductionVO,
  outputs: FinishProductionVO[],
  step?: ProcessStep,
): PrintRouteOutput[] {
  const items = outputs.map(finishRouteOutput)
  const trim = fallbackSingleStageTrim(production, outputs, step)
  return withLayerTexts(trim ? [...items, trim] : items, buildFinishLayers(production, outputs))
}

function finishRouteOutput(finish: FinishProductionVO): PrintRouteOutput {
  const remain = isRemainProductionFinish(finish)
  return {
    key: finish.uuid,
    name: remain ? trimTitle(finish.finishRollNo) : finish.finishRollNo || '预生成成品',
    spec: finishSpec(finish),
    weight: formatKg(finish.estimateWeight),
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
  if ((step?.stepType ?? production.mainStepType) == null) return null
  const trimWidth = calcTrimWidth(production)
  const trimWeight = trimWeightFromFinishes(production.finishes)
  if (trimWidth <= 0 && trimWeight <= 0) return null
  const sourceWeight = (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  const estimateWeight = trimWeight > 0 ? trimWeight : estimateTrimWeight(sourceWeight, production.originalWidth, trimWidth)
  return {
    key: `${production.originalUuid ?? 'roll'}-trim`,
    name: '修边',
    spec: trimWidth > 0 ? `${trimWidth}mm` : '-',
    weight: estimateWeight == null ? '-' : formatKg(estimateWeight),
    width: trimWidth,
    status: 'trim',
  }
}

function routeOutput(output: StageOutputVO): PrintRouteOutput {
  const trim = isTrimOutput(output)
  return {
    key: output.uuid,
    name: trim ? trimTitle(output.outputNo) : output.outputNo || '-',
    spec: outputSpec(output),
    weight: formatKg(output.estimateWeight),
    width: output.finishWidth,
    status: trim ? 'trim' : isFinalOutput(output) ? 'final' : 'next',
  }
}

function layeredRouteOutputs(
  production: RollProductionVO,
  rows: PrintRouteOutput[],
  outputs: StageOutputVO[],
  step?: ProcessStep,
): PrintRouteOutput[] {
  if ((step?.stepType ?? outputs[0]?.sourceStepType) !== 2) return rows
  return withLayerTexts(rows, buildStageOutputLayers(production, outputs))
}

function withLayerTexts<T>(
  rows: PrintRouteOutput[],
  layers: Array<LayeredRewindLayer<T>>,
): PrintRouteOutput[] {
  if (!layers.length) return rows
  const map = layerItemMap(layers)
  const fallbackTrimLayer = layers.find((layer) => layer.trimWidth > 0)
  return rows.map((row) => {
    const layer = map.get(row.key) ?? (row.status === 'trim' ? fallbackTrimLayer : undefined)
    return layer ? { ...row, layerText: layer.label } : row
  })
}

function outputsWithTrim(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): StageOutputVO[] {
  const stepType = step?.stepType ?? stageOutputs[0]?.sourceStepType
  if (stepType !== 1) return stageOutputs
  if (stageOutputs.some(isTrimOutput)) return stageOutputs
  const source = stageSource(production, stageOutputs, allOutputs)
  const trim = trimOutput({
    key: `${production.originalUuid ?? 'roll'}-stage-${stageOutputs[0]?.stageLevel ?? 1}-trim`,
    sourceWeight: source.weight,
    sourceWidth: source.width,
    usedWidth: stageOutputs
      .filter((item) => !isTrimOutput(item))
      .reduce((sum, item) => sum + (item.finishWidth ?? 0), 0),
  })
  return trim ? [...stageOutputs, trimToStageOutput(trim, stageOutputs[0])] : stageOutputs
}

function stageSource(
  production: RollProductionVO,
  stageOutputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): { width?: number; weight: number } {
  const parentUuid = stageOutputs.find((item) => item.parentOutputUuid)?.parentOutputUuid
  const parent = allOutputs.find((item) => item.uuid === parentUuid)
  if (parent) return { width: parent.finishWidth, weight: parent.estimateWeight ?? 0 }
  return {
    width: production.originalWidth,
    weight: (production.rollWeight ?? 0) * (production.pieceNum ?? 1),
  }
}

function trimOutput(params: {
  key: string
  sourceWeight: number
  sourceWidth?: number
  usedWidth: number
}): PrintRouteOutput | null {
  if (!params.sourceWidth || params.usedWidth <= 0) return null
  const trimWidth = params.sourceWidth - params.usedWidth
  if (trimWidth <= 0) return null
  const trimWeight = params.sourceWeight > 0 ? params.sourceWeight * trimWidth / params.sourceWidth : undefined
  return {
    key: params.key,
    name: '修边',
    spec: `${trimWidth}mm`,
    weight: trimWeight == null ? '-' : formatKg(trimWeight),
    status: 'trim',
  }
}

function estimateTrimWeight(sourceWeight: number, sourceWidth?: number, trimWidth?: number): number | undefined {
  if (!sourceWidth || !trimWidth || sourceWeight <= 0) return undefined
  return sourceWeight * trimWidth / sourceWidth
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

function stageRequirement(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): string {
  const stepType = step?.stepType ?? outputs[0]?.sourceStepType ?? production.mainStepType
  if (stepType === 1) return sawRequirement(production, step, outputs, allOutputs)
  if (stepType === 2) return rewindRequirement(production, step, outputs, allOutputs)
  return '按本阶段产物规格加工，完成后填写实际重量和异常说明。'
}

function singleStageRequirement(
  production: RollProductionVO,
  outputs: FinishProductionVO[],
  step?: ProcessStep,
): string {
  const stepType = step?.stepType ?? production.mainStepType
  const deliverableOutputs = outputs.filter(isDeliverableProductionFinish)
  if (stepType === 1) {
    const sourceWidth = production.originalWidth
    const sourceWeight = (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
    return sawText({
      sourceWidth,
      sourceWeight,
      knifeCount: step?.knifeCount,
      outputs: deliverableOutputs.map((item) => ({ width: item.finishWidth, status: 'final' })),
    })
  }
  const text = rewindText({
    source: '原卷',
    sourceWeight: (production.rollWeight ?? 0) * (production.pieceNum ?? 1),
    outputs: deliverableOutputs.map((item) => ({
      width: item.finishWidth,
      diameter: item.finishDiameter,
      core: item.finishCoreDiameter,
    })),
  })
  return appendLayerRequirement(text, layersSummaryText(buildFinishLayers(production, outputs)))
}

function sawRequirement(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): string {
  const source = stageSource(production, outputs, allOutputs)
  return sawText({
    sourceWidth: source.width,
    sourceWeight: source.weight,
    knifeCount: step?.knifeCount,
    outputs: outputs.filter((item) => !isTrimOutput(item)).map((item) => ({
      width: item.finishWidth,
      status: isFinalOutput(item) ? 'final' : 'next',
    })),
  })
}

function rewindRequirement(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): string {
  const source = sourceText(step, allOutputs, outputs)
  const text = rewindText({
    source,
    sourceWeight: rewindSourceWeight(outputs, allOutputs, step),
    outputs: outputs.filter((item) => !isTrimOutput(item)).map((item) => ({
      width: item.finishWidth,
      diameter: item.finishDiameter,
      core: item.finishCoreDiameter,
    })),
  })
  return appendLayerRequirement(text, layersSummaryText(buildStageOutputLayers(production, outputs)))
}

function appendLayerRequirement(text: string, layerText: string): string {
  if (!layerText) return text
  return `${text.replace(/。$/, '')}；内外层分层：${layerText}。`
}

function sawText(params: {
  sourceWidth?: number
  sourceWeight: number
  knifeCount?: number
  outputs: Array<{ width?: number; status: 'next' | 'final' }>
}): string {
  const usedWidth = params.outputs.reduce((sum, item) => sum + (item.width ?? 0), 0)
  const trimWidth = params.sourceWidth == null ? 0 : Math.max(0, params.sourceWidth - usedWidth)
  const layout = groupedWidths(params.outputs)
  const sourceWidth = params.sourceWidth ? `原幅 ${params.sourceWidth}mm` : '按来源门幅'
  const knife = params.knifeCount != null ? `刀数 ${params.knifeCount} 刀` : '刀数按排布执行'
  const trim = trimWidth > 0
    ? `切边 ${trimWidth}mm${params.sourceWeight > 0 ? ` / ${formatKg(params.sourceWeight * trimWidth / (params.sourceWidth ?? 1))}` : ''}`
    : '无切边'
  return `${sourceWidth}，锯成 ${layout || '按产物表规格'}，${knife}，${trim}。`
}

function rewindText(params: {
  source: string
  sourceWeight: number
  outputs: Array<{ width?: number; diameter?: number; core?: number }>
}): string {
  const sourceWeight = params.sourceWeight > 0 ? `，来源重量 ${formatRawTon(params.sourceWeight)}` : ''
  const layout = groupedRewindOutputs(params.outputs)
  return `复卷 ${params.source}${sourceWeight}，产出 ${layout || '按产物表规格'}。`
}

function groupedWidths(outputs: Array<{ width?: number; status: 'next' | 'final' }>): string {
  const groups = new Map<string, { text: string; count: number; status: 'next' | 'final' }>()
  for (const output of outputs) {
    const text = output.width ? `${output.width}mm` : '未填门幅'
    const key = `${text}-${output.status}`
    const existing = groups.get(key)
    groups.set(key, {
      text,
      status: output.status,
      count: (existing?.count ?? 0) + 1,
    })
  }
  return Array.from(groups.values()).map((item) => {
    return `${item.text} ×${item.count}（${outputStatusLabel(item.status)}）`
  }).join(' + ')
}

function groupedRewindOutputs(outputs: Array<{ width?: number; diameter?: number; core?: number }>): string {
  const groups = new Map<string, { text: string; count: number }>()
  for (const output of outputs) {
    const text = [
      output.width ? `${output.width}mm` : '沿用门幅',
      output.diameter ? fmtDiameter(output.diameter, 'φ') : '直径按配置',
      output.core ? `纸芯 ${output.core}` : '纸芯按配置',
    ].join(' / ')
    groups.set(text, { text, count: (groups.get(text)?.count ?? 0) + 1 })
  }
  return Array.from(groups.values()).map((item) => `${item.text} ×${item.count}`).join(' + ')
}

function outputStatusLabel(status: 'next' | 'final'): string {
  return status === 'next' ? '进入下道' : '最终交付'
}

function rewindSourceWeight(
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
  step?: ProcessStep,
): number {
  if (step?.processWeight != null && step.processWeight > 0) return step.processWeight
  const parentWeightKg = sumParentWeights(outputs, allOutputs)
  if (parentWeightKg > 0) return parentWeightKg / 1000
  return outputs.reduce((sum, output) => sum + (output.estimateWeight ?? 0), 0) / 1000
}

function sourceItems(production: RollProductionVO): Array<{ label: string; value: string }> {
  return [
    { label: '卷号/编号', value: [production.rollNo, production.extraNo].filter(Boolean).join(' / ') || '-' },
    { label: '品名', value: production.paperName || '-' },
    { label: '克重/门幅', value: `${production.gramWeight ?? '-'}g / ${production.originalWidth ?? '-'}mm` },
    { label: '标重', value: formatKg((production.rollWeight ?? 0) * (production.pieceNum ?? 1)) },
    { label: '方式', value: `${PROCESS_MODE[production.processMode ?? 1] ?? '-'} / ${STEP_TYPE[production.mainStepType ?? 1] ?? '-'}` },
  ]
}

function activeStageOutputs(outputs?: StageOutputVO[]): StageOutputVO[] {
  return (outputs ?? [])
    .filter((item) => item.outputStatus !== 4)
    .sort((a, b) => (a.stageLevel ?? 0) - (b.stageLevel ?? 0) || (a.outputSort ?? 0) - (b.outputSort ?? 0))
}

function sortedSteps(steps?: ProcessStep[]): ProcessStep[] {
  return [...(steps ?? [])].sort((a, b) => (a.stepSort ?? 0) - (b.stepSort ?? 0))
}

function mergedSteps(production: RollProductionVO, detailSteps: ProcessStep[]): ProcessStep[] {
  const map = new Map<string, ProcessStep>()
  for (const step of production.steps ?? []) map.set(step.uuid, step)
  for (const step of detailSteps) {
    if (step.originalUuid === production.originalUuid) map.set(step.uuid, step)
  }
  return sortedSteps(Array.from(map.values()))
}

function routeLevels(steps: ProcessStep[], outputs: StageOutputVO[]): number[] {
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
): ProcessStep | undefined {
  return steps.find((item) => item.stageLevel === level)
    ?? steps[index]
    ?? steps.find((item) => item.stepType === outputs[0]?.sourceStepType)
}

function stageTitle(level: number, step?: ProcessStep, outputs: StageOutputVO[] = []): string {
  const type = step?.stepType ?? outputs[0]?.sourceStepType
  return `第${level}道 ${step?.stepName || STEP_TYPE[type ?? 1] || '加工'}`
}

function sourceText(step: ProcessStep | undefined, outputs: StageOutputVO[], stageOutputs: StageOutputVO[]): string {
  const inputUuid = step?.inputOutputUuid || stageOutputs.find((item) => item.parentOutputUuid)?.parentOutputUuid
  if (!inputUuid) return '原卷'
  const source = outputs.find((item) => item.uuid === inputUuid)
  return source?.outputNo ? `产物 ${source.outputNo}` : '上一阶段产物'
}

function stepMetric(step: ProcessStep | undefined, stageOutputs: StageOutputVO[], allOutputs: StageOutputVO[]): string {
  if (!step) return fallbackMetric(stageOutputs, allOutputs)
  if (step.stepType === 1) return `刀数 ${step.knifeCount ?? 0} 刀`
  if (step.stepType === 2) return `复卷 ${formatRawTon(step.processWeight)}`
  return '-'
}

function fallbackMetric(stageOutputs: StageOutputVO[], allOutputs: StageOutputVO[]): string {
  const stepType = stageOutputs[0]?.sourceStepType
  if (stepType === 1) return '刀数 -'
  if (stepType !== 2) return '-'
  const parentWeight = sumParentWeights(stageOutputs, allOutputs)
  return parentWeight > 0 ? `复卷 ${formatTon(parentWeight)}` : '复卷 -'
}

function sumParentWeights(stageOutputs: StageOutputVO[], allOutputs: StageOutputVO[]): number {
  const parentUuids = new Set(stageOutputs.map((item) => item.parentOutputUuid).filter(Boolean))
  return allOutputs.reduce((sum, output) => {
    return parentUuids.has(output.uuid) ? sum + (output.estimateWeight ?? 0) : sum
  }, 0)
}

function isFinalOutput(output: StageOutputVO): boolean {
  return output.outputStatus === 3 || output.outputType === 2
}

function isTrimOutput(output: StageOutputVO): boolean {
  return output.isRemain === 1
    || output.outputNo === '切边'
    || output.outputNo === '修边'
    || output.paperName === '切边'
    || output.paperName === '修边'
    || output.paperName === '修边/余料'
    || output.remark === '修边/余料'
}

function isFinalFinishRoll(item: { isSpare?: number; isRemain?: number; rollNoStatus?: number }): boolean {
  return item.isSpare !== 1 && item.isRemain !== 1 && item.rollNoStatus !== 3
}

function trimTitle(identifier?: string) {
  if (!identifier || identifier === '修边' || identifier === '切边') return '修边'
  return `修边 ${identifier}`
}

function rollTitle(seq: number, production: RollProductionVO, isMergeGroup: boolean): string {
  if (isMergeGroup) return `合并复卷 ${seq}`
  return production.rollNo || production.extraNo || `母卷 ${seq}`
}

function outputSpec(output: StageOutputVO): string {
  return specText(output.paperName, output.gramWeight, output.finishWidth, output.finishDiameter, output.finishCoreDiameter)
}

function finishSpec(finish: FinishProductionVO): string {
  return specText(finish.paperName, finish.gramWeight, finish.finishWidth, finish.finishDiameter, finish.finishCoreDiameter)
}

function specText(paperName?: string, gramWeight?: number, width?: number, diameter?: number, core?: number): string {
  const parts = [
    paperName || '-',
    gramWeight ? `${gramWeight}g` : undefined,
    width ? `${width}mm` : undefined,
    diameter ? fmtDiameter(diameter, 'φ') : undefined,
    core ? `纸芯 ${core}` : undefined,
  ].filter(Boolean)
  return parts.join(' / ')
}

function sumKnifeCount(steps?: ProcessStep[]): number {
  return (steps ?? []).reduce((sum, step) => step.stepType === 1 ? sum + (step.knifeCount ?? 0) : sum, 0)
}
