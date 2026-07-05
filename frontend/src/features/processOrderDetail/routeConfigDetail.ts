import type {
  FinishConfigSpecDTO,
  FinishProductionVO,
  OriginalRoll,
  ProcessPlanDTO,
  ProcessRouteOutputDTO,
  ProcessRoutePreviewDTO,
  ProcessRouteStageDTO,
  RewindLayoutItemPlanDTO,
  RewindSegmentPlanDTO,
  RollProductionVO,
  StageOutputVO,
} from '../../types/processOrder'
import type { RollDraft } from '../processOrderCreate/types'

export const STEP_TYPE_SAW = 1
export const STEP_TYPE_REWIND = 2

export interface DetailRouteFormState {
  baseStageLevel: number
  firstOutputs: DetailRouteOutputRow[]
  firstSawKnifeCount?: number
  firstStepType: number
  firstUnitPrice?: number
  preferredSourceOutputKey?: string
  stages: DetailRouteStageForm[]
}

export interface DetailRouteStageForm {
  id: string
  inputOutputKeys: string[]
  plan: ProcessPlanDTO
  stageLevel: number
  stepType: number
}

export interface DetailRouteOutputRow {
  estimateWeight: number
  finishCoreDiameter?: number
  finishDiameter?: number
  finishRollNo?: string
  finishWidth: number
  gramWeight?: number
  label: string
  outputKey: string
  parentOutputKey?: string
  parentOutputUuid?: string
  paperName?: string
  sourceStepType?: number
  sourceOutputKey?: string
  sourceRollNo?: string
  sourceSummary?: string
  stageLevel: number
}

export interface DetailRoutePriceDefaults {
  rewindUnitPrice?: number
  sawUnitPrice?: number
}

const ROLL_NO_VOID = 3
const OUTPUT_CONSUMED = 2
const OUTPUT_VOID = 4

export function initialDetailRouteForm(
  roll: OriginalRoll,
  production?: RollProductionVO,
  prices: DetailRoutePriceDefaults = {},
  baseStageLevel = 1,
  useCurrentOutputs = false,
  preferredSourceOutputKey?: string,
): DetailRouteFormState {
  const firstStepType = resolveFirstStepType(roll, production)
  return {
    baseStageLevel,
    firstOutputs: buildFirstOutputs(roll, production, baseStageLevel, useCurrentOutputs),
    firstSawKnifeCount: production?.steps?.find((step) => step.stepType === STEP_TYPE_SAW)?.knifeCount ?? 1,
    firstStepType,
    firstUnitPrice: priceForStep(firstStepType, prices),
    preferredSourceOutputKey,
    stages: [],
  }
}

export function addDetailRouteStage(
  form: DetailRouteFormState,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): DetailRouteFormState {
  const source = nextStageDefaultSource(form)
  if (!source) return form
  const stageLevel = nextStageLevel(source)
  const stage = newStage(stageLevel, source, stepType, prices)
  return { ...form, stages: [...form.stages, stage] }
}

export function changeDetailRouteStageInputs(
  form: DetailRouteFormState,
  stageId: string,
  inputOutputKeys: string[],
  prices: DetailRoutePriceDefaults,
): DetailRouteFormState {
  return updateStage(form, stageId, (stage) => {
    const sources = sourceRowsFromKeys(inputRowsForStage(form, stage.id), inputOutputKeys)
    if (!sources.length) return stage
    return { ...stage, inputOutputKeys: sources.map((row) => row.outputKey), plan: defaultPlanForSources(sources, stage.stepType, prices) }
  })
}

export function changeDetailRouteStageType(
  form: DetailRouteFormState,
  stageId: string,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): DetailRouteFormState {
  return updateStage(form, stageId, (stage) => {
    const sources = sourceRowsFromKeys(inputRowsForStage(form, stage.id), stage.inputOutputKeys)
    if (!sources.length) return { ...stage, stepType }
    return { ...stage, stepType, plan: defaultPlanForSources(sources, stepType, prices) }
  })
}

export function updateDetailRouteStagePlan(
  form: DetailRouteFormState,
  stageId: string,
  plan: ProcessPlanDTO,
): DetailRouteFormState {
  return updateStage(form, stageId, (stage) => ({ ...stage, plan }))
}

export function removeLastDetailRouteStage(form: DetailRouteFormState): DetailRouteFormState {
  return { ...form, stages: form.stages.slice(0, -1) }
}

export function inputRowsForStage(form: DetailRouteFormState, stageId: string): DetailRouteOutputRow[] {
  const index = form.stages.findIndex((stage) => stage.id === stageId)
  const stageIndex = index < 0 ? form.stages.length : index
  const currentInputs = new Set(index >= 0 ? form.stages[index]?.inputOutputKeys ?? [] : [])
  const consumed = consumedInputKeysBeforeStage(form, stageIndex)
  return outputRowsBeforeStage(form, stageIndex)
    .filter((row) => !consumed.has(row.outputKey) || currentInputs.has(row.outputKey))
}

export function stageOutputRows(form: DetailRouteFormState, stage: DetailRouteStageForm): DetailRouteOutputRow[] {
  const beforeRows = outputRowsBeforeStage(form, stageIndexOf(form, stage.id))
  const sources = sourceRowsFromKeys(beforeRows, stage.inputOutputKeys)
  if (!sources.length) return []
  return outputsForPlan(stage.stageLevel, sources, stage.plan, beforeRows)
}

export function selectedInputRowsForStage(form: DetailRouteFormState, stage: DetailRouteStageForm): DetailRouteOutputRow[] {
  return sourceRowsFromKeys(inputRowsForStage(form, stage.id), stage.inputOutputKeys)
}

export function finalDetailRouteOutputs(form: DetailRouteFormState): DetailRouteOutputRow[] {
  const consumed = new Set(form.stages.flatMap((stage) => stage.inputOutputKeys))
  return allDetailRouteOutputs(form).filter((row) => !consumed.has(row.outputKey))
}

export function allDetailRouteOutputs(form: DetailRouteFormState): DetailRouteOutputRow[] {
  return outputRowsBeforeStage(form, form.stages.length)
}

export function sourceRollFromOutput(row: DetailRouteOutputRow): RollDraft {
  return {
    localId: row.outputKey,
    uuid: row.outputKey,
    paperName: row.paperName ?? '',
    gramWeight: row.gramWeight ?? 0,
    originalWidth: row.finishWidth,
    originalDiameter: row.finishDiameter,
    coreDiameter: row.finishCoreDiameter,
    rollWeight: row.estimateWeight,
    pieceNum: 1,
    processMode: 1,
    mainStepType: STEP_TYPE_REWIND,
  }
}

export function routeOutputRowsForPlan(
  stageLevel: number,
  sources: DetailRouteOutputRow[],
  plan: ProcessPlanDTO,
  existingRows: DetailRouteOutputRow[] = [],
): DetailRouteOutputRow[] {
  return outputsForPlan(stageLevel, sources, plan, existingRows)
}

export function buildDetailRouteDto(roll: OriginalRoll, form: DetailRouteFormState): ProcessRoutePreviewDTO {
  return {
    originalUuid: roll.uuid,
    stages: [firstStage(form), ...form.stages.map((stage) => stageDto(form, stage))],
  }
}

export function buildAppendRouteDto(roll: OriginalRoll, form: DetailRouteFormState): ProcessRoutePreviewDTO {
  return {
    originalUuid: roll.uuid,
    stages: form.stages.map((stage) => stageDto(form, stage)),
  }
}

export function routeStepName(stepType?: number): string {
  if (stepType === STEP_TYPE_SAW) return '锯纸'
  if (stepType === STEP_TYPE_REWIND) return '复卷'
  return '未配置'
}

function newStage(
  stageLevel: number,
  source: DetailRouteOutputRow,
  stepType: number,
  prices: DetailRoutePriceDefaults,
): DetailRouteStageForm {
  return {
    id: `stage-${stageLevel}-${source.outputKey}`,
    inputOutputKeys: [source.outputKey],
    plan: defaultPlanForSources([source], stepType, prices),
    stageLevel,
    stepType,
  }
}

function defaultPlanForSources(
  sources: DetailRouteOutputRow[],
  stepType: number,
  prices: DetailRoutePriceDefaults,
): ProcessPlanDTO {
  const source = combinedSource(sources)
  if (stepType === STEP_TYPE_SAW) return defaultSawPlan(source, prices)
  return defaultRewindPlan(sources, prices)
}

function defaultSawPlan(source: DetailRouteOutputRow, prices: DetailRoutePriceDefaults): ProcessPlanDTO {
  const split = splitOutput(source)
  return {
    processMode: 1,
    mainStepType: STEP_TYPE_SAW,
    knifeCount: 1,
    unitPrice: prices.sawUnitPrice,
    finishSpecs: [
      finishSpec(source, split.firstWidth, split.firstWeight),
      finishSpec(source, split.secondWidth, split.secondWeight),
    ],
  }
}

function defaultRewindPlan(sources: DetailRouteOutputRow[], prices: DetailRoutePriceDefaults): ProcessPlanDTO {
  const source = combinedSource(sources)
  return {
    processMode: 1,
    mainStepType: STEP_TYPE_REWIND,
    rewindMode: sources.length > 1 ? 5 : 2,
    unitPrice: prices.rewindUnitPrice,
    segments: [defaultRewindSegment(source, sources)],
  }
}

function finishSpec(source: DetailRouteOutputRow, finishWidth: number, estimateWeight: number): FinishConfigSpecDTO {
  return {
    itemType: 'FINISH',
    count: 1,
    finishWidth,
    estimateWeight,
    finishCoreDiameter: source.finishCoreDiameter,
    finishDiameter: source.finishDiameter,
  }
}

function defaultRewindSegment(source: DetailRouteOutputRow, sources: DetailRouteOutputRow[] = [source]): RewindSegmentPlanDTO {
  return {
    segmentSort: 1,
    segmentRatio: 1,
    targetDiameter: source.finishDiameter,
    finishCoreDiameter: source.finishCoreDiameter ?? 3,
    repeatCount: 1,
    sources: sources.map((row, index) => ({
      originalUuid: row.outputKey,
      shareRatio: roundPercent(row.estimateWeight, source.estimateWeight),
      consumeRatio: 100,
      sourceSort: index + 1,
    })),
    layoutItems: [{ width: source.finishWidth, quantity: 1, itemType: 'FINISH' }],
  }
}

function outputRowsBeforeStage(form: DetailRouteFormState, stageIndex: number): DetailRouteOutputRow[] {
  return form.stages.slice(0, stageIndex).reduce((rows, stage) => {
    const sources = sourceRowsFromKeys(rows, stage.inputOutputKeys)
    return sources.length ? [...rows, ...outputsForPlan(stage.stageLevel, sources, stage.plan, rows)] : rows
  }, form.firstOutputs)
}

function consumedInputKeysBeforeStage(form: DetailRouteFormState, stageIndex: number): Set<string> {
  return new Set(form.stages.slice(0, stageIndex).flatMap((stage) => stage.inputOutputKeys))
}

function nextStageDefaultSource(form: DetailRouteFormState) {
  const lastStage = form.stages.at(-1)
  if (lastStage) {
    const lastOutputs = stageOutputRows(form, lastStage)
    if (lastOutputs.length) return lastOutputs[0]
  }
  const finals = finalDetailRouteOutputs(form)
  return preferredSource(finals, form.preferredSourceOutputKey) ?? finals[0]
}

function preferredSource(rows: DetailRouteOutputRow[], preferredKey?: string) {
  if (!preferredKey) return undefined
  return rows.find((row) => row.outputKey === preferredKey || row.finishRollNo === preferredKey)
}

function nextStageLevel(source: DetailRouteOutputRow) {
  return (source.stageLevel || 1) + 1
}

function stageIndexOf(form: DetailRouteFormState, stageId: string) {
  const index = form.stages.findIndex((stage) => stage.id === stageId)
  return index < 0 ? form.stages.length : index
}

function outputsForPlan(
  stageLevel: number,
  sources: DetailRouteOutputRow[],
  plan: ProcessPlanDTO,
  existingRows: DetailRouteOutputRow[] = [],
): DetailRouteOutputRow[] {
  const source = combinedSource(sources)
  const seeds = plan.mainStepType === STEP_TYPE_SAW ? sawOutputSeeds(source, plan) : rewindOutputSeeds(sources, plan)
  const usedKeys = new Set(existingRows.map((row) => row.outputKey))
  return seeds.map((seed, index) => ({
    ...seed,
    label: `第${stageLevel}段产物 ${index + 1}`,
    outputKey: nextOutputKey(stageLevel, usedKeys),
    parentOutputKey: source.outputKey,
    sourceStepType: plan.mainStepType,
    sourceOutputKey: source.outputKey,
    sourceSummary: routeStepName(plan.mainStepType),
    stageLevel,
  }))
}

function nextOutputKey(stageLevel: number, usedKeys: Set<string>) {
  let sort = 1
  while (usedKeys.has(`S${stageLevel}-F${sort}`)) {
    sort += 1
  }
  const key = `S${stageLevel}-F${sort}`
  usedKeys.add(key)
  return key
}

function sawOutputSeeds(source: DetailRouteOutputRow, plan: ProcessPlanDTO): OutputSeed[] {
  const specs = (plan.finishSpecs ?? []).filter((spec) => (spec.itemType ?? 'FINISH') !== 'TRIM')
  const expandedSpecs = specs.flatMap((spec) => Array.from({ length: Math.max(1, spec.count ?? 1) }, () => spec))
  const finishWeight = Math.max(0, source.estimateWeight - sawTrimWeight(source, plan.finishSpecs ?? []))
  const widthTotal = expandedSpecs.reduce((sum, spec) => sum + Number(spec.finishWidth ?? 0), 0)
  let allocated = 0
  const rows = expandedSpecs.map((spec, index) => {
    const estimateWeight = allocatedSawWeight(finishWeight, widthTotal, Number(spec.finishWidth ?? 0), index, expandedSpecs.length, allocated)
    allocated += estimateWeight
    return {
      estimateWeight,
      finishCoreDiameter: spec.finishCoreDiameter ?? source.finishCoreDiameter,
      finishDiameter: spec.finishDiameter ?? source.finishDiameter,
      finishWidth: Number(spec.finishWidth ?? source.finishWidth),
      gramWeight: source.gramWeight,
      paperName: source.paperName,
    }
  })
  return rows.length ? rows : [seedFromSource(source)]
}

function allocatedSawWeight(totalWeight: number, widthTotal: number, width: number, index: number, count: number, allocated: number) {
  if (count <= 0 || totalWeight <= 0 || widthTotal <= 0) return 0
  if (index === count - 1) return roundWeight(totalWeight - allocated)
  return roundWeight(totalWeight * width / widthTotal)
}

function sawTrimWeight(source: DetailRouteOutputRow, specs: FinishConfigSpecDTO[]) {
  const trimWidth = specs
    .filter((spec) => spec.itemType === 'TRIM')
    .reduce((sum, spec) => sum + Number(spec.finishWidth ?? 0) * Math.max(1, spec.count ?? 1), 0)
  if (trimWidth <= 0 || source.finishWidth <= 0) return 0
  return roundWeight(source.estimateWeight * trimWidth / source.finishWidth)
}

function rewindOutputSeeds(sources: DetailRouteOutputRow[], plan: ProcessPlanDTO): OutputSeed[] {
  const source = combinedSource(sources)
  const segments = plan.segments?.length ? plan.segments : [defaultRewindSegment(source)]
  const ratios = rewindSegmentRatios(sources, segments, plan.rewindMode)
  const totalWeight = rewindTotalWeight(sources, segments, plan.rewindMode)
  return allocateRewindSeeds(
    segments.flatMap((segment, index) => rewindSegmentSeedInputs(source, segment, ratios[index] ?? 0, plan.rewindMode)),
    totalWeight,
    source.finishWidth,
    rewindTrimWidth(segments, source.finishWidth, ratios),
  )
}

function rewindSegmentSeedInputs(
  source: DetailRouteOutputRow,
  segment: RewindSegmentPlanDTO,
  segmentRatio: number,
  rewindMode?: number,
): RewindSeedInput[] {
  const items = (segment.layoutItems ?? []).filter((item) => item.itemType !== 'TRIM')
  const repeatCount = Math.max(1, Number(segment.repeatCount ?? 1))
  const repeatedRatio = segmentRatio / repeatCount
  return items.flatMap((item) => Array.from({ length: repeatCount * Math.max(1, item.quantity ?? 1) }, () => ({
    basis: rewindSeedBasis(source, segment, item, repeatedRatio, rewindMode),
    finishCoreDiameter: segment.finishCoreDiameter ?? firstLayerCoreDiameter(item.layers) ?? source.finishCoreDiameter,
    finishDiameter: segment.targetDiameter ?? maxLayerOutDiameter(item.layers) ?? source.finishDiameter,
    finishWidth: item.width,
    gramWeight: source.gramWeight,
    paperName: source.paperName,
  })))
}

function allocateRewindSeeds(inputs: RewindSeedInput[], totalWeight: number, originalWidth: number, trimWidth: number): OutputSeed[] {
  if (!inputs.length) return []
  const trimWeight = originalWidth > 0 ? totalWeight * trimWidth / originalWidth : 0
  const allocatableWeight = totalWeight - trimWeight
  const basisTotal = inputs.reduce((sum, input) => sum + input.basis, 0)
  let allocated = 0
  return inputs.map((input, index) => {
    const estimateWeight = index === inputs.length - 1
      ? roundWeight(allocatableWeight - allocated)
      : roundWeight(basisTotal > 0 ? allocatableWeight * input.basis / basisTotal : allocatableWeight / inputs.length)
    allocated += estimateWeight
    return { ...input, estimateWeight }
  })
}

function rewindSeedBasis(
  source: DetailRouteOutputRow,
  segment: RewindSegmentPlanDTO,
  item: RewindLayoutItemPlanDTO,
  ratio: number,
  rewindMode?: number,
) {
  const finishWidth = item.width
  const layeredBasis = layoutLayerArea(item.layers)
  if (rewindMode === 4 && layeredBasis > 0) return layeredBasis * ratio
  const area = layerArea(segment.targetDiameter, segment.finishCoreDiameter)
  if (rewindMode === 2) return (area > 0 ? area : finishWidth) * ratio
  if (rewindMode === 3 || rewindMode === 4) return (area > 0 ? area : source.finishWidth) * safeDivide(finishWidth, source.finishWidth) * ratio
  return finishWidth * ratio
}

function rewindTrimWidth(segments: RewindSegmentPlanDTO[], originalWidth: number, ratios: number[]) {
  return segments.reduce((sum, segment, index) => sum + segmentTrimWidth(segment, originalWidth) * (ratios[index] ?? 0), 0)
}

function rewindSegmentRatios(sources: DetailRouteOutputRow[], segments: RewindSegmentPlanDTO[], rewindMode?: number) {
  if (hasSourceConsumption(segments, rewindMode)) {
    const total = totalConsumedWeight(sources, segments)
    if (total > 0) return segments.map((segment) => safeDivide(segmentConsumedWeight(sources, segment), total))
  }
  const totalRatio = segments.reduce((sum, segment) => sum + Number(segment.segmentRatio ?? 1), 0) || 1
  return segments.map((segment) => Number(segment.segmentRatio ?? 1) / totalRatio)
}

function rewindTotalWeight(sources: DetailRouteOutputRow[], segments: RewindSegmentPlanDTO[], rewindMode?: number) {
  if (!hasSourceConsumption(segments, rewindMode)) return combinedSource(sources).estimateWeight
  const consumed = totalConsumedWeight(sources, segments)
  return consumed > 0 ? consumed : combinedSource(sources).estimateWeight
}

function hasSourceConsumption(segments: RewindSegmentPlanDTO[], rewindMode?: number) {
  return rewindMode === 5 && segments.some((segment) => segment.sources?.some((source) => source.consumeRatio != null))
}

function totalConsumedWeight(sources: DetailRouteOutputRow[], segments: RewindSegmentPlanDTO[]) {
  return segments.reduce((sum, segment) => sum + segmentConsumedWeight(sources, segment), 0)
}

function segmentConsumedWeight(sources: DetailRouteOutputRow[], segment: RewindSegmentPlanDTO) {
  return (segment.sources ?? []).reduce((sum, source) => {
    const row = sources.find((item) => item.outputKey === source.originalUuid)
    const ratio = Number(source.consumeRatio ?? source.shareRatio ?? 0) / 100
    return sum + (row?.estimateWeight ?? 0) * ratio
  }, 0)
}

function segmentTrimWidth(segment: RewindSegmentPlanDTO, originalWidth: number) {
  const explicitTrim = layoutWidth(segment, 'TRIM')
  if (explicitTrim > 0) return explicitTrim
  return Math.max(0, originalWidth - layoutWidth(segment, 'FINISH'))
}

function layoutWidth(segment: RewindSegmentPlanDTO, type: 'FINISH' | 'TRIM') {
  return (segment.layoutItems ?? [])
    .filter((item) => (item.itemType ?? 'FINISH') === type)
    .reduce((sum, item) => sum + item.width * Math.max(1, Number(item.quantity ?? 1)), 0)
}

function layerArea(targetDiameter?: number, coreDiameter?: number) {
  if (!targetDiameter || !coreDiameter) return 0
  const outRadius = targetDiameter * 25.4 / 2
  const coreRadius = coreDiameter * 25.4 / 2
  return Math.PI * (outRadius * outRadius - coreRadius * coreRadius)
}

function layoutLayerArea(layers?: { outDiameter?: number; coreDiameter?: number }[]) {
  return (layers ?? []).reduce((sum, layer) => sum + layerArea(layer.outDiameter, layer.coreDiameter), 0)
}

function maxLayerOutDiameter(layers?: { outDiameter?: number }[]) {
  return Math.max(0, ...(layers ?? []).map((layer) => Number(layer.outDiameter ?? 0))) || undefined
}

function firstLayerCoreDiameter(layers?: { coreDiameter?: number }[]) {
  return layers?.find((layer) => Number(layer.coreDiameter ?? 0) > 0)?.coreDiameter
}

function safeDivide(value: number, divisor: number) {
  return divisor ? value / divisor : 0
}

function firstStage(form: DetailRouteFormState): ProcessRouteStageDTO {
  return {
    stageLevel: 1,
    stepType: form.firstStepType,
    stepName: routeStepName(form.firstStepType),
    knifeCount: form.firstStepType === STEP_TYPE_SAW ? form.firstSawKnifeCount : undefined,
    unitPrice: form.firstUnitPrice,
    outputs: form.firstOutputs.map(toOutputDto),
  }
}

function stageDto(form: DetailRouteFormState, stage: DetailRouteStageForm): ProcessRouteStageDTO {
  return {
    stageLevel: stage.stageLevel,
    inputOutputKeys: stage.inputOutputKeys,
    stepType: stage.stepType,
    stepName: routeStepName(stage.stepType),
    knifeCount: stage.stepType === STEP_TYPE_SAW ? stage.plan.knifeCount : undefined,
    unitPrice: stage.plan.unitPrice,
    plan: stage.plan,
    outputs: stageOutputRows(form, stage).map(toOutputDto),
  }
}

function toOutputDto(row: DetailRouteOutputRow): ProcessRouteOutputDTO {
  return {
    outputKey: row.outputKey,
    paperName: row.paperName,
    gramWeight: row.gramWeight,
    finishCoreDiameter: row.finishCoreDiameter,
    finishDiameter: row.finishDiameter,
    finishWidth: row.finishWidth,
    estimateWeight: row.estimateWeight,
  }
}

function buildFirstOutputs(
  roll: OriginalRoll,
  production: RollProductionVO | undefined,
  baseStageLevel: number,
  useCurrentOutputs: boolean,
): DetailRouteOutputRow[] {
  const allStageOutputs = activeStageOutputs(production?.stageOutputs)
  const stageOutputs = routeStageOutputs(production?.stageOutputs, useCurrentOutputs)
  const rows = [
    ...stageOutputs.map((output, index) => stageOutputRow(roll, output, index, baseStageLevel)),
    ...unlinkedFinishRows(roll, production, baseStageLevel, allStageOutputs),
  ]
  return rows.length ? rows : [originalAsOutput(roll, baseStageLevel)]
}

function unlinkedFinishRows(
  roll: OriginalRoll,
  production: RollProductionVO | undefined,
  baseStageLevel: number,
  stageOutputs: StageOutputVO[],
): DetailRouteOutputRow[] {
  const linkedKeys = linkedFinishKeys(stageOutputs)
  const stageLevel = stageOutputs.length ? 1 : baseStageLevel
  return activeFinishes(production?.finishes ?? [])
    .filter((finish) => !finishLinkedToStageOutput(finish, linkedKeys))
    .map((finish, index) => finishOutputRow(roll, finish, index, stageLevel))
}

function finishOutputRow(
  roll: OriginalRoll,
  finish: FinishProductionVO,
  index: number,
  stageLevel: number,
): DetailRouteOutputRow {
  return {
    estimateWeight: finishWeight(roll, finish.estimateWeight ?? finish.actualWeight, finish.finishWidth),
    finishCoreDiameter: finish.finishCoreDiameter,
    finishDiameter: finish.finishDiameter,
    finishRollNo: finish.finishRollNo,
    finishWidth: Number(finish.finishWidth ?? roll.originalWidth ?? 1),
    gramWeight: finish.gramWeight ?? roll.gramWeight,
    label: `${stageLabel(stageLevel)}产物 ${index + 1}`,
    outputKey: finishOutputKey(finish, index),
    paperName: finish.paperName ?? roll.paperName,
    sourceRollNo: finish.finishRollNo,
    stageLevel,
  }
}

function routeStageOutputs(outputs: StageOutputVO[] | undefined, useCurrentOutputs: boolean): StageOutputVO[] {
  const active = activeStageOutputs(outputs)
  if (useCurrentOutputs) return active.filter((output) => output.outputStatus !== OUTPUT_CONSUMED)
  return active.filter((output) => (output.stageLevel ?? 1) === 1)
}

function activeStageOutputs(outputs: StageOutputVO[] | undefined): StageOutputVO[] {
  return (outputs ?? []).filter((output) => output.outputStatus !== OUTPUT_VOID)
}

function linkedFinishKeys(outputs: StageOutputVO[]) {
  const keys = new Set<string>()
  for (const output of outputs) {
    if (output.finishRollUuid) keys.add(output.finishRollUuid)
    if (output.outputNo) keys.add(output.outputNo)
  }
  return keys
}

function finishLinkedToStageOutput(finish: FinishProductionVO, linkedKeys: Set<string>) {
  return Boolean(
    (finish.uuid && linkedKeys.has(finish.uuid))
    || (finish.finishRollNo && linkedKeys.has(finish.finishRollNo)),
  )
}

function stageOutputRow(
  roll: OriginalRoll,
  output: StageOutputVO,
  index: number,
  baseStageLevel: number,
): DetailRouteOutputRow {
  const stageLevel = output.stageLevel ?? baseStageLevel
  return {
    estimateWeight: finishWeight(roll, output.estimateWeight ?? output.actualWeight, output.finishWidth),
    finishCoreDiameter: output.finishCoreDiameter,
    finishDiameter: output.finishDiameter,
    finishWidth: Number(output.finishWidth ?? roll.originalWidth ?? 1),
    gramWeight: output.gramWeight ?? roll.gramWeight,
    label: `${stageLabel(stageLevel)}产物 ${output.outputSort ?? index + 1}`,
    outputKey: output.outputNo || output.uuid,
    parentOutputUuid: output.parentOutputUuid,
    paperName: output.paperName ?? roll.paperName,
    sourceStepType: output.sourceStepType,
    sourceOutputKey: output.outputNo,
    sourceRollNo: output.outputNo,
    sourceSummary: output.sourceSummary,
    stageLevel,
  }
}

function activeFinishes(finishes: FinishProductionVO[]): FinishProductionVO[] {
  return finishes.filter((finish) => finish.rollNoStatus !== ROLL_NO_VOID)
}

function finishOutputKey(finish: FinishProductionVO, index: number) {
  return finish.finishRollNo || (finish.uuid ? `F:${finish.uuid}` : `S1-F${index + 1}`)
}

function originalAsOutput(roll: OriginalRoll, stageLevel: number): DetailRouteOutputRow {
  return {
    estimateWeight: rollTotalWeight(roll),
    finishCoreDiameter: roll.coreDiameter,
    finishDiameter: roll.originalDiameter,
    finishWidth: Number(roll.originalWidth ?? 1),
    gramWeight: roll.gramWeight,
    label: `${stageLabel(stageLevel)}产物 1`,
    outputKey: 'S1-F1',
    paperName: roll.paperName,
    sourceRollNo: roll.rollNo || roll.extraNo,
    stageLevel,
  }
}

function stageLabel(stageLevel: number) {
  return stageLevel <= 1 ? '首道' : `第${stageLevel}段`
}

function updateStage(
  form: DetailRouteFormState,
  stageId: string,
  updater: (stage: DetailRouteStageForm) => DetailRouteStageForm,
): DetailRouteFormState {
  return { ...form, stages: form.stages.map((stage) => (stage.id === stageId ? updater(stage) : stage)) }
}

function splitOutput(row: DetailRouteOutputRow) {
  const firstWidth = Math.max(1, Math.floor(row.finishWidth / 2))
  const firstWeight = roundWeight(row.estimateWeight / 2)
  return {
    firstWidth,
    firstWeight,
    secondWidth: Math.max(1, row.finishWidth - firstWidth),
    secondWeight: roundWeight(row.estimateWeight - firstWeight),
  }
}

function resolveFirstStepType(roll: OriginalRoll, production?: RollProductionVO) {
  return production?.steps?.find((step) => step.isMain === 1)?.stepType
    ?? production?.steps?.[0]?.stepType
    ?? roll.mainStepType
    ?? STEP_TYPE_REWIND
}

function priceForStep(stepType: number, prices: DetailRoutePriceDefaults) {
  return stepType === STEP_TYPE_SAW ? prices.sawUnitPrice : prices.rewindUnitPrice
}

function seedFromSource(source: DetailRouteOutputRow): OutputSeed {
  return {
    estimateWeight: source.estimateWeight,
    finishCoreDiameter: source.finishCoreDiameter,
    finishDiameter: source.finishDiameter,
    finishWidth: source.finishWidth,
    gramWeight: source.gramWeight,
    paperName: source.paperName,
  }
}

function rollTotalWeight(roll: OriginalRoll) {
  return Number(roll.actualWeight ?? roll.totalWeight ?? (Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)))
}

function finishWeight(roll: OriginalRoll, value?: number, width?: number) {
  const explicit = Number(value ?? 0)
  if (explicit > 0) return explicit
  return roundWeight(rollTotalWeight(roll) * Number(width ?? roll.originalWidth ?? 1) / Number(roll.originalWidth ?? 1))
}

function roundWeight(value: number) {
  return Number(value.toFixed(3))
}

function combinedSource(sources: DetailRouteOutputRow[]): DetailRouteOutputRow {
  const [first] = sources
  if (!first) return emptySource()
  if (sources.length === 1) return first
  const keys = sources.map((row) => row.outputKey)
  return {
    ...first,
    estimateWeight: roundWeight(sources.reduce((sum, row) => sum + row.estimateWeight, 0)),
    label: `${sources.length}个来源合并`,
    outputKey: keys.join('+'),
    sourceOutputKey: keys.join('、'),
    sourceRollNo: sources.map((row) => row.finishRollNo || row.sourceRollNo || row.outputKey).join('、'),
  }
}

function emptySource(): DetailRouteOutputRow {
  return { estimateWeight: 0, finishWidth: 1, label: '未选择来源', outputKey: '', stageLevel: 1 }
}

function sourceRowsFromKeys(rows: DetailRouteOutputRow[], keys: string[]): DetailRouteOutputRow[] {
  const keySet = new Set(keys)
  return rows.filter((row) => keySet.has(row.outputKey))
}

function roundPercent(weight: number, total: number) {
  if (!total) return 0
  return Number((weight * 100 / total).toFixed(2))
}

interface OutputSeed {
  estimateWeight: number
  finishCoreDiameter?: number
  finishDiameter?: number
  finishWidth: number
  gramWeight?: number
  paperName?: string
}

interface RewindSeedInput extends Omit<OutputSeed, 'estimateWeight'> {
  basis: number
}
