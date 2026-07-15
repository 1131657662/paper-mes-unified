import { IS_INVOICE, ORDER_SETTLE_TYPE, PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import {
  calcTrimWidth,
  isDeliverableProductionFinish,
  isRemainProductionFinish,
  isVisibleProductionOutput,
  trimWeightFromFinishes,
} from '../../../components/processOrder/shared/detailHelpers'
import {
  buildFinishLayers,
  buildStageOutputLayers,
  layerItemMap,
  type LayeredRewindLayer,
} from '../../../components/processOrder/shared/layeredRewindView'
import {
  compareLayerRows,
  layerItemSortMap,
  type LayeredRewindSort,
} from '../../../components/processOrder/shared/layeredRewindOrder'
import {
  sortFinishOutputs,
  sortStageOutputs,
} from '../../../components/processOrder/shared/outputOrder'
import type {
  FinishProductionVO,
  ProcessOrderDetailVO,
  ProcessStep,
  RollProductionVO,
  StageOutputVO,
} from '../../../types/processOrder'
import { formatGram, formatMm, formatOptionalTon as formatRawTon } from '../../../utils/numberFormatters'
import { buildDetailMetrics, formatProductionKg, formatTon } from '../orderDetailUtils'

export interface PrintRouteOutput {
  key: string
  layerText?: string
  name: string
  spec: string
  weight: string
  actualWeight?: string
  weightValue?: number
  width?: number
  status: 'next' | 'final' | 'trim'
}

export interface PrintRouteStage {
  key: string
  stepType?: number
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

interface PrintRouteSortContext<T> {
  fallbackTrimLayer?: LayeredRewindLayer<T>
  sortMap: Map<string, LayeredRewindSort>
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
      stepType: step?.stepType ?? levelOutputs[0]?.sourceStepType,
      title: stageTitle(level, step, levelOutputs),
      source: sourceText(step, outputs, levelOutputs),
      metric: stepMetric(step, levelOutputs, outputs),
      requirement: stageRequirement(production, step, levelOutputs, outputs),
      outputs: layeredRouteOutputs(
        production,
        stageOutputsWithTrim.map((output) => routeOutput(output, production)),
        stageOutputsWithTrim,
        step,
      ),
    }
  })
}

function routeStagesFromFinishes(production: RollProductionVO, steps: ProcessStep[]): PrintRouteStage[] {
  const step = sortedSteps(steps)[0]
  const outputs = sortFinishOutputs(
    production,
    (production.finishes ?? []).filter(isVisibleProductionOutput),
  )
  return [{
    key: `${production.originalUuid ?? 'roll'}-single`,
    stepType: step?.stepType ?? production.mainStepType,
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
  const sortedOutputs = sortFinishOutputs(production, outputs)
  const items = sortedOutputs.map((finish) => finishRouteOutput(finish, production))
  const trim = fallbackSingleStageTrim(production, outputs, step)
  return withLayerTexts(trim ? [...items, trim] : items, buildFinishLayers(production, sortedOutputs))
}

function finishRouteOutput(finish: FinishProductionVO, production: RollProductionVO): PrintRouteOutput {
  const remain = isRemainProductionFinish(finish)
  return {
    key: finish.uuid,
    name: remain ? trimTitle(finish.finishRollNo) : finish.finishRollNo || '预生成成品',
    spec: finishSpec(finish),
    weight: formatProductionKg(finish.estimateWeight, production),
    actualWeight: finish.actualWeight == null ? undefined : formatProductionKg(finish.actualWeight, production),
    weightValue: finish.estimateWeight,
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
    spec: trimWidth > 0 ? formatMm(trimWidth) : '-',
    weight: estimateWeight == null ? '-' : formatProductionKg(estimateWeight, production),
    weightValue: estimateWeight,
    width: trimWidth,
    status: 'trim',
  }
}

function routeOutput(output: StageOutputVO, production: RollProductionVO): PrintRouteOutput {
  const trim = isTrimOutput(output)
  return {
    key: output.uuid,
    name: trim ? trimTitle(output.outputNo) : output.outputNo || '-',
    spec: outputSpec(output),
    weight: formatProductionKg(output.estimateWeight, production),
    actualWeight: output.actualWeight == null ? undefined : formatProductionKg(output.actualWeight, production),
    weightValue: output.estimateWeight,
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
  if ((step?.stepType ?? outputs[0]?.sourceStepType) !== 2) {
    return sortRowsByStageOutputs(production, rows, outputs)
  }
  const layers = buildStageOutputLayers(production, outputs)
  return layers.length ? withLayerTexts(rows, layers) : sortRowsByStageOutputs(production, rows, outputs)
}

function sortRowsByStageOutputs(
  production: RollProductionVO,
  rows: PrintRouteOutput[],
  outputs: StageOutputVO[],
): PrintRouteOutput[] {
  const order = new Map(sortStageOutputs(production, outputs).map((output, index) => [output.uuid, index]))
  return [...rows].sort((a, b) => (order.get(a.key) ?? 0) - (order.get(b.key) ?? 0))
}

function withLayerTexts<T>(
  rows: PrintRouteOutput[],
  layers: Array<LayeredRewindLayer<T>>,
): PrintRouteOutput[] {
  if (!layers.length) return rows
  const map = layerItemMap(layers)
  const sortMap = layerItemSortMap(layers)
  const fallbackTrimLayer = layers.find((layer) => layer.trimWidth > 0)
  const sortContext = { fallbackTrimLayer, sortMap }
  return rows.map((row) => {
    const layer = map.get(row.key) ?? (row.status === 'trim' ? fallbackTrimLayer : undefined)
    return layer ? { ...row, layerText: layer.label } : row
  }).sort((a, b) => comparePrintRouteOutputs(a, b, sortContext))
}

function comparePrintRouteOutputs<T>(
  a: PrintRouteOutput,
  b: PrintRouteOutput,
  context: PrintRouteSortContext<T>,
): number {
  return compareLayerRows(
    outputSortInfo(a, context),
    outputSortInfo(b, context),
  ) || (a.width ?? 0) - (b.width ?? 0) || a.name.localeCompare(b.name)
}

function outputSortInfo<T>(
  row: PrintRouteOutput,
  context: PrintRouteSortContext<T>,
): LayeredRewindSort | undefined {
  return context.sortMap.get(row.key)
    ?? (row.status === 'trim' && context.fallbackTrimLayer
      ? fallbackTrimSort(context.sortMap, context.fallbackTrimLayer.index)
      : undefined)
}

function fallbackTrimSort(
  sortMap: Map<string, LayeredRewindSort>,
  layerIndex: number,
): LayeredRewindSort {
  const layerOrders = Array.from(sortMap.values()).filter((item) => item.layerIndex === layerIndex)
  const maxOrder = Math.max(0, ...layerOrders.map((item) => item.displayOrder))
  return { displayOrder: maxOrder + 0.5, layerIndex }
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
    production,
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
  production: RollProductionVO
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
    spec: formatMm(trimWidth),
    weight: trimWeight == null ? '-' : formatProductionKg(trimWeight, params.production),
    weightValue: trimWeight,
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
  if (stepType === 2) return rewindRequirement(outputs)
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
      production,
      sourceWidth,
      sourceWeight,
      knifeCount: step?.knifeCount,
      outputs: deliverableOutputs.map((item) => ({ width: item.finishWidth, status: 'final' })),
    })
  }
  const text = rewindText({
    outputs: deliverableOutputs.map((item) => ({ width: item.finishWidth })),
  })
  return text
}

function sawRequirement(
  production: RollProductionVO,
  step: ProcessStep | undefined,
  outputs: StageOutputVO[],
  allOutputs: StageOutputVO[],
): string {
  const source = stageSource(production, outputs, allOutputs)
  return sawText({
    production,
    sourceWidth: source.width,
    sourceWeight: source.weight,
    knifeCount: step?.knifeCount,
    outputs: outputs.filter((item) => !isTrimOutput(item)).map((item) => ({
      width: item.finishWidth,
      status: isFinalOutput(item) ? 'final' : 'next',
    })),
  })
}

function rewindRequirement(outputs: StageOutputVO[]): string {
  const text = rewindText({
    outputs: outputs.filter((item) => !isTrimOutput(item)).map((item) => ({
      width: item.finishWidth,
    })),
  })
  return text
}

function sawText(params: {
  production: RollProductionVO
  sourceWidth?: number
  sourceWeight: number
  knifeCount?: number
  outputs: Array<{ width?: number; status: 'next' | 'final' }>
}): string {
  const usedWidth = params.outputs.reduce((sum, item) => sum + (item.width ?? 0), 0)
  const trimWidth = params.sourceWidth == null ? 0 : Math.max(0, params.sourceWidth - usedWidth)
  const layout = groupedWidths(params.outputs)
  const sourceWidth = params.sourceWidth ? `原幅 ${formatMm(params.sourceWidth)}` : '按来源门幅'
  const derivedKnifeCount = Math.max(0, params.outputs.length - 1) + (trimWidth > 0 ? 1 : 0)
  const knifeCount = params.knifeCount != null && params.knifeCount > 0
    ? params.knifeCount
    : derivedKnifeCount
  const knife = knifeCount > 0 ? `共 ${knifeCount} 刀` : '刀数按排布执行'
  const trim = trimWidth > 0
    ? `切边 ${formatMm(trimWidth)}${params.sourceWeight > 0 ? `，约 ${formatProductionKg(params.sourceWeight * trimWidth / (params.sourceWidth ?? 1), params.production)}` : ''}`
    : '无切边'
  return `${sourceWidth}；产出 ${layout || '按产物表规格'}；${trim}；${knife}。`
}

function rewindText(params: {
  outputs: Array<{ width?: number }>
}): string {
  const layout = groupedRewindOutputs(params.outputs)
  return layout || '复卷门幅按产物表执行。'
}

function groupedWidths(outputs: Array<{ width?: number; status: 'next' | 'final' }>): string {
  const groups = new Map<string, { text: string; count: number; status: 'next' | 'final' }>()
  for (const output of outputs) {
    const text = output.width ? formatMm(output.width) : '未填门幅'
    const key = `${text}-${output.status}`
    const existing = groups.get(key)
    groups.set(key, {
      text,
      status: output.status,
      count: (existing?.count ?? 0) + 1,
    })
  }
  return Array.from(groups.values()).map((item) => {
    const nextStage = item.status === 'next' ? '（进下道）' : ''
    return `${item.text} ×${item.count} 件${nextStage}`
  }).join(' + ')
}

function groupedRewindOutputs(outputs: Array<{ width?: number }>): string {
  const groups = new Map<string, { text: string; count: number }>()
  for (const output of [...outputs].sort((a, b) => (a.width ?? 0) - (b.width ?? 0))) {
    const text = output.width ? formatMm(output.width) : '沿用门幅'
    groups.set(text, { text, count: (groups.get(text)?.count ?? 0) + 1 })
  }
  const items = Array.from(groups.values())
  const totalCount = items.reduce((sum, item) => sum + item.count, 0)
  const onlyItem = items[0]
  if (items.length === 1 && onlyItem) return `加工门幅 ${onlyItem.text}，产出 ${totalCount} 件。`
  const widths = items.map((item) => `${item.text} ×${item.count} 件`).join(' + ')
  return `加工门幅 ${widths}，共产出 ${totalCount} 件。`
}

function sourceItems(production: RollProductionVO): Array<{ label: string; value: string }> {
  const gramWeight = production.actualGramWeight ?? production.gramWeight
  const width = production.actualWidth ?? production.originalWidth
  const weight = production.actualWeight ?? (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  const gramText = `${formatGram(gramWeight)}${production.actualGramWeight == null ? '' : '（实）'}`
  const widthText = `${formatMm(width)}${production.actualWidth == null ? '' : '（实）'}`
  return [
    { label: '卷号/编号', value: [production.rollNo, production.extraNo].filter(Boolean).join(' / ') || '-' },
    { label: '品名', value: production.paperName || '-' },
    { label: '克重/门幅', value: `${gramText} / ${widthText}` },
    { label: production.actualWeight == null ? '标重' : '实重', value: formatProductionKg(weight, production) },
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
  return specText(output.paperName, output.gramWeight, output.finishWidth)
}

function finishSpec(finish: FinishProductionVO): string {
  return specText(finish.paperName, finish.gramWeight, finish.finishWidth)
}

function specText(paperName?: string, gramWeight?: number, width?: number): string {
  const parts = [
    paperName || '-',
    gramWeight ? formatGram(gramWeight) : undefined,
    width ? formatMm(width) : undefined,
  ].filter(Boolean)
  return parts.join(' / ')
}

function sumKnifeCount(steps?: ProcessStep[]): number {
  return (steps ?? []).reduce((sum, step) => step.stepType === 1 ? sum + (step.knifeCount ?? 0) : sum, 0)
}
