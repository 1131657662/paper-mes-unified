import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import { buildProcessingFlow } from '../../../components/processOrder/shared/detailHelpers'
import { compareFinishProductions } from '../../../components/processOrder/shared/productionSpecificationOrder'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import type { OriginalRoll, ProcessOrderDetailVO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import { activeFinishRolls, type BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem, BackRecordWorkbenchData, WorkbenchFinish } from './backRecordWorkbenchTypes'
import { addedFinishEntities, isFinishProduced, normalizeFinishAdjustment } from './backRecordFinishAdjustment'
import { sourceWeightSummary } from './backRecordSourceRolls'
import { requiresMeasuredSourceWeights } from './backRecordWeightPolicy'

export interface WorkItemMetrics {
  rollActual?: number
  finishActual: number
  productActual: number
  trimActual: number
  loss: number
  scrap: number
  missingRoll: boolean
  missingRolls: number
  unverifiedRolls: number
  missingFinishes: number
  missingFinishWidths: number
  diff?: number
  diffRatio?: number
}

export function buildBackRecordWorkbench(detail: ProcessOrderDetailVO): BackRecordWorkbenchData {
  const rows = buildDisplayRows(detail.rollProductions ?? [])
  const rollSequence = buildRollSequence(detail.originalRolls)
  const items = rows.length > 0
    ? rows.map((row) => fromDisplayRow(row, detail, rollSequence))
    : fromOriginalRolls(detail)
  attachFinishes(items, detail)
  return { items: appendPool(items, detail) }
}

export function buildWorkItemMetrics(
  item: BackRecordWorkItem,
  values: BackRecordFormValues,
): WorkItemMetrics {
  if (item.roll?.processMode === 2) return buildOnSiteMetrics(item, values)
  const sourceWeights = sourceWeightSummary(item, values)
  const rollActual = sourceWeights.completeTotal
  const requiredWeight = requiresMeasuredSourceWeights(item)
  const measuredMissing = sourceWeights.measuredMissingCount
  const weightMissing = requiredWeight ? measuredMissing : sourceWeights.missingCount
  const adjustment = normalizeFinishAdjustment(item, values.finishAdjustments?.[item.key])
  const official = item.finishes.filter(({ finish }) => finish.isSpare !== 1 && isFinishProduced(finish.uuid, adjustment))
  const products = official.filter(({ finish }) => finish.isRemain !== 1)
  const added = addedFinishEntities(item, adjustment)
  const remains = official.filter(({ finish }) => finish.isRemain === 1)
  const trims = values.trims?.[item.key] ?? []
  const productActual = sum(products.map(({ finish }) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
    + sum(added.map((finish) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
  const trimActual = sum(remains.map(({ finish }) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
    + sum(trims.map((trim) => trim.actualWeight))
  const finishActual = productActual + trimActual
  const loss = sum(processSteps(item).map((step) => values.steps?.[step.uuid]?.lossWeight ?? step.lossWeight))
  const scrap = sum(item.finishes.map(({ finish }) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight))
    + sum(added.map((finish) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight))
  const missingFinishes = products.filter(({ finish }) => !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length
    + added.filter((finish) => !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length
    + trims.filter((trim) => !positive(trim.actualWeight)).length
  const missingFinishWidths = item.roll?.processMode === 2
    ? official.filter(({ finish }) => !positive(values.finishes?.[finish.uuid]?.finishWidth ?? validWidth(finish.finishWidth))).length
    : 0
  const missingTrimWidths = trims.filter((trim) => !positive(trim.finishWidth)).length
  const diff = rollActual == null ? undefined : rollActual - finishActual - loss - scrap

  return {
    rollActual,
    finishActual,
    productActual,
    trimActual,
    loss,
    scrap,
    missingRoll: item.kind === 'roll' && requiredWeight && weightMissing > 0,
    missingRolls: requiredWeight ? weightMissing : 0,
    unverifiedRolls: requiredWeight ? 0 : measuredMissing,
    missingFinishes,
    missingFinishWidths: missingFinishWidths + missingTrimWidths,
    diff,
    diffRatio: rollActual != null && rollActual > 0 && diff != null ? Math.abs(diff) / rollActual : undefined,
  }
}

function buildOnSiteMetrics(item: BackRecordWorkItem, values: BackRecordFormValues): WorkItemMetrics {
  const sourceWeights = sourceWeightSummary(item, values)
  const rollActual = sourceWeights.completeTotal
  const requiredWeight = requiresMeasuredSourceWeights(item)
  const measuredMissing = sourceWeights.measuredMissingCount
  const weightMissing = requiredWeight ? measuredMissing : sourceWeights.missingCount
  const outputs = (values.onSiteOutputs?.[item.key] ?? [])
    .filter((output): output is NonNullable<typeof output> => output != null)
  const products = outputs.filter((output) => output.outputType === 'FINISH')
  const trims = outputs.filter((output) => output.outputType === 'TRIM')
  const productActual = sum(products.map((output) => output.actualWeight))
  const trimActual = sum(trims.map((output) => output.actualWeight))
  const finishActual = productActual + trimActual
  const loss = sum(processSteps(item).map((step) => values.steps?.[step.uuid]?.lossWeight ?? step.lossWeight))
  const scrap = sum(products.map((output) => output.scrapWeight))
  const missingRows = outputs.filter((output) => !positive(output.actualWeight)).length
  const missingWidths = outputs.filter((output) => !positive(output.finishWidth)).length
  const diff = rollActual == null ? undefined : rollActual - finishActual - loss - scrap
  return {
    rollActual,
    finishActual,
    productActual,
    trimActual,
    loss,
    scrap,
    missingRoll: requiredWeight && weightMissing > 0,
    missingRolls: requiredWeight ? weightMissing : 0,
    unverifiedRolls: requiredWeight ? 0 : measuredMissing,
    missingFinishes: products.length === 0 ? missingRows + 1 : missingRows,
    missingFinishWidths: missingWidths,
    diff,
    diffRatio: rollActual != null && rollActual > 0 && diff != null ? Math.abs(diff) / rollActual : undefined,
  }
}

export function workItemStatus(item: BackRecordWorkItem, values: BackRecordFormValues) {
  if (item.kind === 'pool') return { text: '待核对', color: 'warning' }
  if (item.roll?.dispositionAction === 'CANCEL') return { text: '已取消', color: 'default' }
  if (item.roll?.dispositionAction === 'SPLIT_TO_ORDER') return { text: '已转代加工', color: 'cyan' }
  if (item.roll?.dispositionAction === 'DIRECT_SHIP') return { text: '已转直发', color: 'blue' }
  if (workItemRecorded(item)) return { text: '已入库', color: 'default' }
  if (item.roll?.processMode === 3) return { text: '直发', color: 'blue' }
  const metrics = buildWorkItemMetrics(item, values)
  if (metrics.missingRoll || metrics.missingFinishes > 0 || metrics.missingFinishWidths > 0) {
    return { text: item.roll?.processMode === 4 ? '待录整理结果' : '待补', color: 'warning' }
  }
  if (item.roll?.processMode === 4) return { text: '整理已录', color: 'success' }
  return { text: '已录', color: 'success' }
}

export function workItemRollUuids(item: BackRecordWorkItem): string[] {
  const uuids = item.rollProductions
    .map((production) => production.originalUuid)
    .filter((uuid): uuid is string => Boolean(uuid))
  if (uuids.length) return Array.from(new Set(uuids))
  return item.roll?.uuid ? [item.roll.uuid] : []
}

export function workItemRecorded(item: BackRecordWorkItem): boolean {
  const productions = item.rollProductions.length
    ? item.rollProductions
    : item.roll ? [{ isChecked: item.roll.isChecked }] : []
  return productions.length > 0 && productions.every((production) => production.isChecked === 1)
}

export function processLines(item: BackRecordWorkItem): Array<{ header: string; details: string[] }> {
  if (!item.production) return [{ header: '未保存工艺方案', details: [] }]
  return buildProcessingFlow(item.production)
}

function fromDisplayRow(
  row: DisplayRow,
  detail: ProcessOrderDetailVO,
  rollSequence: Map<string, number>,
): BackRecordWorkItem {
  const roll = detail.originalRolls.find((item) => item.uuid === row.mainProduction.originalUuid)
  const sequence = roll ? rollSequence.get(roll.uuid) ?? row.seq : row.seq
  return {
    key: row.key,
    kind: 'roll',
    title: row.isMergeGroup ? `合并复卷 ${row.rollProductions.length} 卷` : rollName(roll, sequence),
    subtitle: row.rollProductions.map(sourceText).join(' / '),
    roll,
    production: row.mainProduction,
    rollProductions: row.rollProductions,
    isMergeGroup: row.isMergeGroup,
    sourceMode: 'none',
    finishes: [],
  }
}

function fromOriginalRolls(detail: ProcessOrderDetailVO): BackRecordWorkItem[] {
  return detail.originalRolls.map((roll, index) => ({
    key: `roll-${roll.uuid}`,
    kind: 'roll',
    title: rollName(roll, index + 1),
    subtitle: sourceText(roll),
    roll,
    rollProductions: [],
    isMergeGroup: false,
    sourceMode: 'none',
    finishes: [],
  }))
}

function attachFinishes(items: BackRecordWorkItem[], detail: ProcessOrderDetailVO) {
  const active = activeFinishRolls(detail)
  const byUuid = new Map(active.map((finish) => [finish.uuid, finish]))

  for (const item of items) {
    const finishes = [...(item.production?.finishes ?? [])].sort(compareFinishProductions)
    for (const finish of finishes) {
      const matched = byUuid.get(finish.uuid)
      if (!matched) continue
      item.finishes.push({ finish: matched, bindMode: 'linked' })
      item.sourceMode = 'linked'
    }
  }
}

function buildRollSequence(rolls: OriginalRoll[]): Map<string, number> {
  return new Map(
    rolls.map((roll, index) => [roll.uuid, index + 1]),
  )
}

function appendPool(items: BackRecordWorkItem[], detail: ProcessOrderDetailVO): BackRecordWorkItem[] {
  const used = new Set(items.flatMap((item) => item.finishes.map(({ finish }) => finish.uuid)))
  const pool = activeFinishRolls(detail)
    .filter((finish) => !used.has(finish.uuid))
    .sort(compareFinishProductions)
  if (pool.length === 0) return items

  return [...items, {
    key: 'finish-pool',
    kind: 'pool',
    title: '待核对成品池',
    subtitle: '这些成品没有明确来源母卷。',
    rollProductions: [],
    isMergeGroup: false,
    sourceMode: 'pool',
    finishes: pool.map((finish): WorkbenchFinish => ({ finish, bindMode: 'pool' })),
  }]
}

function rollName(_roll: OriginalRoll | undefined, seq: number): string {
  return `母卷 ${seq}`
}

function sourceText(roll: OriginalRoll): string
function sourceText(roll: DisplayRow['mainProduction']): string
function sourceText(roll: OriginalRoll | DisplayRow['mainProduction']): string {
  const paper = roll.paperName || '-'
  const gram = formatGram(roll.gramWeight)
  const width = formatMm(roll.originalWidth)
  return `${paper} / ${gram} / ${width}`
}

function sum(values: Array<number | undefined>): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}

function positive(value?: number) {
  return value != null && value > 0
}

function validWidth(value?: number) {
  return positive(value) ? value : undefined
}

export function processSteps(item: BackRecordWorkItem) {
  const productions = item.rollProductions.length ? item.rollProductions : item.production ? [item.production] : []
  return Array.from(new Map(productions.flatMap((production) => production.steps ?? []).map((step) => [step.uuid, step])).values())
}
