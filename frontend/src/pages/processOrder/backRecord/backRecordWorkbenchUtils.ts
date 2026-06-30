import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import { buildProcessingFlow } from '../../../components/processOrder/shared/detailHelpers'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import type { FinishRoll, OriginalRoll, ProcessOrderDetailVO } from '../../../types/processOrder'
import { activeFinishRolls, type BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem, BackRecordWorkbenchData, WorkbenchFinish } from './backRecordWorkbenchTypes'

export interface WorkItemMetrics {
  rollActual?: number
  finishActual: number
  scrap: number
  missingRoll: boolean
  missingFinishes: number
  diff?: number
  diffRatio?: number
}

export function buildBackRecordWorkbench(detail: ProcessOrderDetailVO): BackRecordWorkbenchData {
  const rows = buildDisplayRows(detail.rollProductions ?? [])
  const items = rows.length > 0 ? rows.map((row) => fromDisplayRow(row, detail)) : fromOriginalRolls(detail)
  attachFinishes(items, detail)
  return { items: appendPool(items, detail) }
}

export function buildWorkItemMetrics(
  item: BackRecordWorkItem,
  values: BackRecordFormValues,
): WorkItemMetrics {
  const rollActual = item.roll ? values.rolls?.[item.roll.uuid]?.actualWeight ?? item.roll.actualWeight : undefined
  const official = item.finishes.filter(({ finish }) => finish.isSpare !== 1)
  const finishActual = sum(official.map(({ finish }) => values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight))
  const scrap = sum(item.finishes.map(({ finish }) => values.finishes?.[finish.uuid]?.scrapWeight ?? finish.scrapWeight))
  const missingFinishes = official.filter(({ finish }) => !positive(values.finishes?.[finish.uuid]?.actualWeight ?? finish.actualWeight)).length
  const diff = rollActual == null ? undefined : rollActual - finishActual - scrap

  return {
    rollActual,
    finishActual,
    scrap,
    missingRoll: item.kind === 'roll' && !positive(rollActual),
    missingFinishes,
    diff,
    diffRatio: rollActual != null && rollActual > 0 && diff != null ? Math.abs(diff) / rollActual : undefined,
  }
}

export function workItemStatus(item: BackRecordWorkItem, values: BackRecordFormValues) {
  if (item.kind === 'pool') return { text: '待核对', color: 'warning' }
  if (item.roll?.processMode === 3) return { text: '直发', color: 'blue' }
  const metrics = buildWorkItemMetrics(item, values)
  if (metrics.missingRoll || metrics.missingFinishes > 0) return { text: '待补', color: 'warning' }
  return { text: '已录', color: 'success' }
}

export function processLines(item: BackRecordWorkItem): Array<{ header: string; details: string[] }> {
  if (!item.production) return [{ header: '未保存工艺方案', details: [] }]
  return buildProcessingFlow(item.production)
}

function fromDisplayRow(row: DisplayRow, detail: ProcessOrderDetailVO): BackRecordWorkItem {
  const roll = detail.originalRolls.find((item) => item.uuid === row.mainProduction.originalUuid)
  return {
    key: row.key,
    kind: 'roll',
    title: row.isMergeGroup ? `合并复卷 ${row.rollProductions.length} 卷` : rollName(roll, row.seq),
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
  const assigned = new Set<string>()

  for (const item of items) {
    for (const finish of item.production?.finishes ?? []) {
      const matched = byUuid.get(finish.uuid)
      if (!matched) continue
      item.finishes.push({ finish: matched, bindMode: 'linked' })
      item.sourceMode = 'linked'
      assigned.add(matched.uuid)
    }
  }

  inferOneToOne(items, active.filter((finish) => !assigned.has(finish.uuid)))
}

function inferOneToOne(items: BackRecordWorkItem[], unassigned: FinishRoll[]) {
  const targets = items.filter((item) => item.kind === 'roll' && item.roll?.processMode !== 3 && item.finishes.length === 0)
  const official = unassigned.filter((finish) => finish.isSpare !== 1)
  const hasLinked = items.some((item) => item.sourceMode === 'linked')
  if (hasLinked || targets.length === 0 || targets.length !== official.length) return

  official
    .sort((a, b) => (a.rowSort ?? 0) - (b.rowSort ?? 0))
    .forEach((finish, index) => {
      targets[index].finishes.push({ finish, bindMode: 'inferred' })
      targets[index].sourceMode = 'inferred'
    })
}

function appendPool(items: BackRecordWorkItem[], detail: ProcessOrderDetailVO): BackRecordWorkItem[] {
  const used = new Set(items.flatMap((item) => item.finishes.map(({ finish }) => finish.uuid)))
  const pool = activeFinishRolls(detail).filter((finish) => !used.has(finish.uuid))
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
  const gram = roll.gramWeight ? `${roll.gramWeight}g` : '-'
  const width = roll.originalWidth ? `${roll.originalWidth}mm` : '-'
  return `${paper} / ${gram} / ${width}`
}

function sum(values: Array<number | undefined>): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0)
}

function positive(value?: number) {
  return value != null && value > 0
}
