import type {
  OriginalRoll,
  ProcessPlanDTO,
  ProcessRouteOutputDTO,
  ProcessRoutePreviewDTO,
  ProcessRouteStageDTO,
  RewindLayoutItemPlanDTO,
  RewindSegmentPlanDTO,
} from '../../types/processOrder'
import { applyLegacyPlanPriceDefaults, defaultPlanForRoll, rollDraftFromOriginal, type DefaultPlanOptions } from '../processOrderCreate/draftMappers'
import type {
  DetailRouteOutputRow,
  DetailRoutePriceDefaults,
} from '../processOrderDetail/routeConfigDetail'
import {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  routeOutputRowsForPlan,
  routeStepName,
  sourceRollFromOutput,
} from '../processOrderDetail/routeConfigDetail'

export interface RouteDraftStage {
  id: string
  inputOutputKeys: string[]
  plan: ProcessPlanDTO
  stageLevel: number
  stepType: number
}

export const ORIGINAL_OUTPUT_KEY = 'ORIGINAL'

export function createRouteStages(
  roll: OriginalRoll,
  route: ProcessRoutePreviewDTO | undefined,
  options: DefaultPlanOptions,
): RouteDraftStage[] {
  if (route?.stages?.length) return route.stages.map((stage) => stageFromDto(stage, roll, options))
  return []
}

export function buildRouteDto(roll: OriginalRoll, stages: RouteDraftStage[]): ProcessRoutePreviewDTO {
  const outputsByStage = buildOutputsByStage(roll, stages)
  return {
    originalUuid: roll.uuid,
    stages: stages.map((stage) => stageDto(stage, outputsByStage.get(stage.id) ?? [])),
  }
}

export function allRouteOutputs(roll: OriginalRoll, stages: RouteDraftStage[]): DetailRouteOutputRow[] {
  return Array.from(buildOutputsByStage(roll, stages).values()).flat()
}

export function routeOutputsByStage(roll: OriginalRoll, stages: RouteDraftStage[]): Map<string, DetailRouteOutputRow[]> {
  return buildOutputsByStage(roll, stages)
}

export function finalRouteOutputs(roll: OriginalRoll, stages: RouteDraftStage[]): DetailRouteOutputRow[] {
  const consumed = new Set(stages.flatMap((stage) => stage.inputOutputKeys))
  return allRouteOutputs(roll, stages).filter((row) => !consumed.has(row.outputKey))
}

export function inputRowsForDraftStage(
  roll: OriginalRoll,
  stages: RouteDraftStage[],
  stageId: string,
): DetailRouteOutputRow[] {
  const stageIndex = stages.findIndex((stage) => stage.id === stageId)
  if (stageIndex <= 0) return [routeOriginalSource(roll)]
  const currentInputs = new Set(stages[stageIndex]?.inputOutputKeys ?? [])
  const priorStages = stages.slice(0, stageIndex)
  const consumedBefore = new Set(priorStages.slice(1).flatMap((stage) => stage.inputOutputKeys))
  return outputsBeforeStage(roll, stages, stageIndex)
    .filter((row) => currentInputs.has(row.outputKey) || !consumedBefore.has(row.outputKey))
}

export function addRouteStage(
  stages: RouteDraftStage[],
  source: DetailRouteOutputRow,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): RouteDraftStage[] {
  const plan = defaultPlanForRouteSource(source, stepType, prices)
  return [...stages, stageShell(nextLevel(source), [source.outputKey], stepType, plan)]
}

export function removeStageAndAfter(stages: RouteDraftStage[], stageId: string): RouteDraftStage[] {
  const index = stages.findIndex((stage) => stage.id === stageId)
  if (index < 0) return stages
  return stages.slice(0, index)
}

export function updateRouteStage(stages: RouteDraftStage[], next: RouteDraftStage): RouteDraftStage[] {
  return stages.map((stage) => (stage.id === next.id ? next : stage))
}

export function stageForSourceKey(stages: RouteDraftStage[], sourceKey: string): RouteDraftStage | undefined {
  if (sourceKey === ORIGINAL_OUTPUT_KEY) {
    return stages.find((stage) => stage.stageLevel <= 1)
  }
  return stages.find((stage) => stage.inputOutputKeys.includes(sourceKey))
}

export function draftStageForSource(
  source: DetailRouteOutputRow,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): RouteDraftStage {
  const inputKeys = source.outputKey === ORIGINAL_OUTPUT_KEY ? [] : [source.outputKey]
  const stageLevel = source.outputKey === ORIGINAL_OUTPUT_KEY ? 1 : nextLevel(source)
  return stageShell(stageLevel, inputKeys, stepType, defaultPlanForRouteSource(source, stepType, prices))
}

export function upsertRouteStage(stages: RouteDraftStage[], next: RouteDraftStage): RouteDraftStage[] {
  return stages.some((stage) => stage.id === next.id) ? updateRouteStage(stages, next) : [...stages, next]
}

export function sourceRowsForRoute(roll: OriginalRoll, stages: RouteDraftStage[]): DetailRouteOutputRow[] {
  return [routeOriginalSource(roll), ...allRouteOutputs(roll, stages)]
}

export function removeOutputFromRoute(roll: OriginalRoll, stages: RouteDraftStage[], outputKey: string) {
  const consumer = stageForSourceKey(stages, outputKey)
  if (consumer) return { changed: true, stages: removeStageAndAfter(stages, consumer.id), message: '已删除该产物的后续工艺' }
  const rowsByStage = buildOutputsByStage(roll, stages)
  const producer = stages.find((stage) => rowsByStage.get(stage.id)?.some((row) => row.outputKey === outputKey))
  if (!producer) return { changed: false, stages, message: '未找到该产物对应的工艺' }
  const rows = rowsByStage.get(producer.id) ?? []
  const outputIndex = rows.findIndex((row) => row.outputKey === outputKey)
  if (rows.length <= 1) return { changed: true, stages: removeStageAndAfter(stages, producer.id), message: '已删除该段工艺' }
  if (outputIndex !== rows.length - 1) {
    return { changed: false, stages, message: '该产物前后还有关联编号，请在右侧工艺参数中调整明细' }
  }
  const plan = planWithoutOutput(producer.plan, outputIndex)
  return { changed: true, stages: updateRouteStage(stages, { ...producer, plan }), message: '已删除该最终产物' }
}

export function routeTotals(roll: OriginalRoll, stages: RouteDraftStage[]) {
  const finals = finalRouteOutputs(roll, stages)
  const stageRows = stages.map((stage) => stageMetric(roll, stages, stage))
  return {
    finishCount: finals.length,
    finishWeight: finals.reduce((sum, row) => sum + Number(row.estimateWeight ?? 0), 0),
    knifeCount: stageRows.reduce((sum, row) => sum + row.knifeCount, 0),
    rewindWeight: stageRows.reduce((sum, row) => sum + row.rewindWeight, 0),
    stageCount: stages.length,
  }
}

function buildOutputsByStage(roll: OriginalRoll, stages: RouteDraftStage[]) {
  const result = new Map<string, DetailRouteOutputRow[]>()
  stages.forEach((stage) => {
    const existingRows = Array.from(result.values()).flat()
    const sources = stage.stageLevel <= 1 ? [routeOriginalSource(roll)] : sourceRows(existingRows, stage.inputOutputKeys)
    result.set(stage.id, routeOutputRowsForPlan(stage.stageLevel, sources, stage.plan, existingRows))
  })
  return result
}

function outputsBeforeStage(roll: OriginalRoll, stages: RouteDraftStage[], stageIndex: number) {
  return Array.from(buildOutputsByStage(roll, stages.slice(0, stageIndex)).values()).flat()
}

function stageDto(stage: RouteDraftStage, outputs: DetailRouteOutputRow[]): ProcessRouteStageDTO {
  return {
    stageLevel: stage.stageLevel,
    inputOutputKeys: stage.stageLevel <= 1 ? undefined : stage.inputOutputKeys,
    stepType: stage.stepType,
    stepName: routeStepName(stage.stepType),
    machineUuid: stage.plan.machineUuid,
    knifeCount: stage.stepType === STEP_TYPE_SAW ? stage.plan.knifeCount : undefined,
    unitPrice: stage.plan.unitPrice,
    plan: stage.plan,
    outputs: outputs.map(outputDto),
  }
}

function outputDto(row: DetailRouteOutputRow): ProcessRouteOutputDTO {
  return {
    outputKey: row.outputKey,
    paperName: row.paperName,
    gramWeight: row.gramWeight,
    finishWidth: row.finishWidth,
    finishDiameter: row.finishDiameter,
    finishCoreDiameter: row.finishCoreDiameter,
    estimateWeight: row.estimateWeight,
  }
}

function stageFromDto(stage: ProcessRouteStageDTO, roll: OriginalRoll, options: DefaultPlanOptions): RouteDraftStage {
  const stepType = stage.stepType ?? STEP_TYPE_REWIND
  const plan = stage.plan ?? planFromOutputs(stage, roll, options)
  return stageShell(stage.stageLevel, stage.inputOutputKeys ?? [], stepType, plan)
}

function planFromOutputs(stage: ProcessRouteStageDTO, roll: OriginalRoll, options: DefaultPlanOptions): ProcessPlanDTO {
  const base = applyLegacyPlanPriceDefaults(defaultPlanForRoll(rollDraftFromOriginal(roll), options), options)
  if (stage.stepType === STEP_TYPE_SAW) return sawPlanFromOutputs(stage, base)
  return rewindPlanFromOutputs(stage, base)
}

function sawPlanFromOutputs(stage: ProcessRouteStageDTO, base: ProcessPlanDTO): ProcessPlanDTO {
  return {
    ...base,
    processMode: 1,
    mainStepType: STEP_TYPE_SAW,
    machineUuid: stage.machineUuid ?? base.machineUuid,
    knifeCount: stage.knifeCount,
    unitPrice: stage.unitPrice,
    finishSpecs: (stage.outputs ?? []).map((output) => ({ ...output, count: output.count ?? 1, itemType: 'FINISH' })),
  }
}

function rewindPlanFromOutputs(stage: ProcessRouteStageDTO, base: ProcessPlanDTO): ProcessPlanDTO {
  const outputs = stage.outputs ?? []
  return {
    ...base,
    processMode: 1,
    mainStepType: STEP_TYPE_REWIND,
    machineUuid: stage.machineUuid ?? base.machineUuid,
    rewindMode: 3,
    unitPrice: stage.unitPrice,
    segments: [{
      segmentSort: 1,
      segmentRatio: 1,
      targetDiameter: outputs[0]?.finishDiameter,
      finishCoreDiameter: outputs[0]?.finishCoreDiameter ?? 3,
      repeatCount: 1,
      layoutItems: outputs.map((output) => ({ width: output.finishWidth ?? 0, quantity: output.count ?? 1, itemType: 'FINISH' })),
    }],
  }
}

export function defaultPlanForRouteSource(row: DetailRouteOutputRow, stepType: number, prices: DetailRoutePriceDefaults): ProcessPlanDTO {
  const source = sourceRollFromOutput(row)
  const plan = defaultPlanForRoll({ ...source, mainStepType: stepType }, {
    sawPrice: prices.sawUnitPrice,
    rewindPrice: prices.rewindUnitPrice,
  })
  return { ...plan, mainStepType: stepType, processMode: 1 }
}

function stageMetric(roll: OriginalRoll, stages: RouteDraftStage[], stage: RouteDraftStage) {
  const inputs = inputRowsForDraftStage(roll, stages, stage.id)
  const rewindInputs = stage.stageLevel <= 1 ? inputs : inputs.filter((row) => stage.inputOutputKeys.includes(row.outputKey))
  return {
    knifeCount: stage.stepType === STEP_TYPE_SAW ? Number(stage.plan.knifeCount ?? 0) : 0,
    rewindWeight: stage.stepType === STEP_TYPE_REWIND
      ? rewindInputs.reduce((sum, row) => sum + row.estimateWeight, 0)
      : 0,
  }
}

function planWithoutOutput(plan: ProcessPlanDTO, outputIndex: number): ProcessPlanDTO {
  if (plan.mainStepType === STEP_TYPE_SAW) {
    return { ...plan, finishSpecs: sawSpecsWithoutOutput(plan, outputIndex) }
  }
  return { ...plan, segments: rewindSegmentsWithoutOutput(plan.segments ?? [], outputIndex) }
}

function sawSpecsWithoutOutput(plan: ProcessPlanDTO, outputIndex: number) {
  const trims = (plan.finishSpecs ?? []).filter((spec) => spec.itemType === 'TRIM')
  const finishes = (plan.finishSpecs ?? [])
    .filter((spec) => (spec.itemType ?? 'FINISH') !== 'TRIM')
    .flatMap((spec) => Array.from({ length: Math.max(1, spec.count ?? 1) }, () => ({ ...spec, count: 1 })))
    .filter((_, index) => index !== outputIndex)
  return [...finishes, ...trims]
}

function rewindSegmentsWithoutOutput(segments: RewindSegmentPlanDTO[], outputIndex: number): RewindSegmentPlanDTO[] {
  const target = rewindOutputTarget(segments, outputIndex)
  if (!target) return segments
  return segments.map((segment, segmentIndex) => {
    if (segmentIndex !== target.segmentIndex) return segment
    return segmentWithoutOutput(segment, target.itemIndex)
  }).filter((segment) => (segment.layoutItems ?? []).some((item) => (item.itemType ?? 'FINISH') === 'FINISH'))
}

function rewindOutputTarget(segments: RewindSegmentPlanDTO[], outputIndex: number) {
  let cursor = 0
  for (let segmentIndex = 0; segmentIndex < segments.length; segmentIndex += 1) {
    const items = segments[segmentIndex]?.layoutItems ?? []
    for (let itemIndex = 0; itemIndex < items.length; itemIndex += 1) {
      const item = items[itemIndex]
      if ((item.itemType ?? 'FINISH') === 'TRIM') continue
      const count = Math.max(1, Number(item.quantity ?? 1)) * Math.max(1, Number(segments[segmentIndex]?.repeatCount ?? 1))
      if (outputIndex < cursor + count) return { itemIndex, segmentIndex }
      cursor += count
    }
  }
  return undefined
}

function segmentWithoutOutput(segment: RewindSegmentPlanDTO, itemIndex: number): RewindSegmentPlanDTO {
  const items = segment.layoutItems ?? []
  const item = items[itemIndex]
  if (!item) return segment
  if (singleRepeatedItem(segment, items, item)) {
    return { ...segment, repeatCount: Math.max(1, Number(segment.repeatCount ?? 1) - 1) }
  }
  return { ...segment, layoutItems: patchLayoutItem(items, itemIndex, item) }
}

function singleRepeatedItem(segment: RewindSegmentPlanDTO, items: RewindLayoutItemPlanDTO[], item: RewindLayoutItemPlanDTO) {
  const finishItems = items.filter((row) => (row.itemType ?? 'FINISH') === 'FINISH')
  return finishItems.length === 1 && Number(segment.repeatCount ?? 1) > 1 && Number(item.quantity ?? 1) === 1
}

function patchLayoutItem(items: RewindLayoutItemPlanDTO[], itemIndex: number, item: RewindLayoutItemPlanDTO): RewindLayoutItemPlanDTO[] {
  if (Number(item.quantity ?? 1) > 1) {
    return items.map((row, index) => (index === itemIndex ? { ...row, quantity: Math.max(1, Number(row.quantity ?? 1) - 1) } : row))
  }
  return items.filter((_, index) => index !== itemIndex)
}

function sourceRows(rows: DetailRouteOutputRow[], keys: string[]) {
  const selected = new Set(keys)
  return rows.filter((row) => selected.has(row.outputKey))
}

export function routeOriginalSource(roll: OriginalRoll): DetailRouteOutputRow {
  return {
    estimateWeight: Number(roll.totalWeight ?? Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)),
    finishCoreDiameter: roll.coreDiameter,
    finishDiameter: roll.originalDiameter,
    finishWidth: Number(roll.originalWidth ?? 1),
    gramWeight: roll.gramWeight,
    label: '母卷',
    outputKey: ORIGINAL_OUTPUT_KEY,
    paperName: roll.paperName,
    sourceRollNo: roll.rollNo || roll.extraNo,
    stageLevel: 0,
  }
}

function stageShell(stageLevel: number, inputOutputKeys: string[], stepType: number, plan: ProcessPlanDTO): RouteDraftStage {
  return { id: `stage-${stageLevel}-${inputOutputKeys.join('-') || 'root'}`, inputOutputKeys, plan, stageLevel, stepType }
}

function nextLevel(source: DetailRouteOutputRow) {
  return Math.max(2, Number(source.stageLevel ?? 1) + 1)
}
