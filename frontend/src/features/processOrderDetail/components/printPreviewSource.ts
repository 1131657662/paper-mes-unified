import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { RollProductionVO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import { formatProductionKg } from '../orderDetailUtils'

export function printSourceItems(
  production: RollProductionVO,
): Array<{ label: string; value: string }> {
  const gramWeight = production.actualGramWeight ?? production.gramWeight
  const width = production.actualWidth ?? production.originalWidth
  const weight = production.actualWeight
    ?? (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  const gramText = `${formatGram(gramWeight)}${production.actualGramWeight == null ? '' : '（实）'}`
  const widthText = `${formatMm(width)}${production.actualWidth == null ? '' : '（实）'}`
  return [
    {
      label: '卷号/编号',
      value: [production.rollNo, production.extraNo].filter(Boolean).join(' / ') || '-',
    },
    { label: '品名', value: production.paperName || '-' },
    { label: '克重/门幅', value: `${gramText} / ${widthText}` },
    {
      label: production.actualWeight == null ? '标重' : '实重',
      value: formatProductionKg(weight, production),
    },
    { label: '方式', value: sourceProcessText(production) },
  ]
}

export function printMergedSourceItems(
  productions: RollProductionVO[],
): Array<{ label: string; value: string }> {
  const entries = productions.map((production, index) => ({
    name: sourceName(production, index),
    items: printSourceItems(production),
    production,
  }))
  const values = (label: string) => entries.map((entry) => sourceValue(entry.items, label))
  return [
    { label: '卷号/编号', value: entries.map((entry) => entry.name).join(' / ') },
    { label: '品名', value: compactValues(entries, values('品名')) },
    { label: '克重/门幅', value: compactValues(entries, values('克重/门幅')) },
    { label: '标重', value: labelledValues(entries, productions.map(sourceWeight)) },
    { label: '方式', value: sourceProcessText(productions[0] ?? {}) },
  ]
}

export function printRollTitle(
  seq: number,
  production: RollProductionVO,
  isMergeGroup: boolean,
) {
  if (isMergeGroup) return `合并复卷 ${seq}`
  return production.rollNo || production.extraNo || `母卷 ${seq}`
}

function sourceProcessText(production: RollProductionVO) {
  const mode = PROCESS_MODE[production.processMode ?? 1] ?? '-'
  if (production.processMode === 3 || production.processMode === 4) return mode
  return `${mode} / ${STEP_TYPE[production.mainStepType ?? 1] ?? '-'}`
}

function sourceName(production: RollProductionVO, index: number) {
  return production.rollNo || production.extraNo || `母卷 ${index + 1}`
}

function sourceWeight(production: RollProductionVO) {
  const weight = production.actualWeight
    ?? (production.rollWeight ?? 0) * (production.pieceNum ?? 1)
  return formatProductionKg(weight, production)
}

function compactValues(
  entries: Array<{ name: string }>,
  values: string[],
) {
  return new Set(values).size === 1 ? values[0] ?? '-' : labelledValues(entries, values)
}

function labelledValues(entries: Array<{ name: string }>, values: string[]) {
  return entries.map((entry, index) => `${entry.name}：${values[index] ?? '-'}`).join('；')
}

function sourceValue(items: Array<{ label: string; value: string }>, label: string) {
  return items.find((item) => item.label === label)?.value ?? '-'
}
