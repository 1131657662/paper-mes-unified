import type { OriginalRoll } from '../../../types/processOrder'
import {
  formatGram,
  formatMm,
  formatOptionalKg,
  formatStoredCoreDiameter,
  formatStoredDiameter,
} from '../../../utils/numberFormatters'
import { isRollWeightKnown, rollTotalWeight } from '../routeConfigSource'

export const DEFAULT_DISPOSITION_VALUES = { action: 'CANCEL' as const }

export interface RollOption {
  label: string
  value: string
  title: string
  searchText: string
  detail: string
  sequence: number
  paperName: string
  identity: string
  statusLabel: string
  fields: RollOptionField[]
  roll: OriginalRoll
}

export interface RollOptionField {
  label: string
  value: string
}

export function buildRollOptions(rolls: OriginalRoll[]): RollOption[] {
  return rolls.map((roll, index) => buildRollOption(roll, index))
}

export function filterRollOption(input: string, option?: RollOption): boolean {
  const query = input.trim().toLowerCase()
  if (!query) return true
  return option?.searchText.toLowerCase().includes(query) ?? false
}

function buildRollOption(roll: OriginalRoll, index: number): RollOption {
  const sequence = roll.rowSort ?? index + 1
  const label = buildRollLabel(roll, index)
  const detail = buildRollDetail(roll)
  return {
    label,
    value: roll.uuid,
    title: `${label} · ${detail}`,
    searchText: `${label} ${detail} ${roll.uuid}`,
    detail,
    sequence,
    paperName: roll.paperName || '-',
    identity: rollIdentity(roll),
    statusLabel: rollStatusLabel(roll),
    fields: buildRollFields(roll),
    roll,
  }
}

function buildRollLabel(roll: OriginalRoll, index: number): string {
  const sequence = roll.rowSort ?? index + 1
  return `母卷 ${sequence} · ${rollIdentity(roll)} · ${roll.paperName || '-'}`
}

function rollIdentity(roll: OriginalRoll): string {
  const values = [
    roll.rollNo ? `卷号 ${roll.rollNo}` : undefined,
    roll.extraNo ? `编号 ${roll.extraNo}` : undefined,
  ].filter((value): value is string => Boolean(value))
  return values.join(' / ') || '未记录卷号'
}

function rollStatusLabel(roll: OriginalRoll): string {
  if (roll.rollStatus === 2) return '加工中'
  if (roll.rollStatus === 1) return '待加工'
  return '未完成'
}

function buildRollFields(roll: OriginalRoll): RollOptionField[] {
  const gram = roll.actualGramWeight ?? roll.gramWeight
  const width = roll.actualWidth ?? roll.originalWidth
  return [
    { label: '卷号', value: roll.rollNo || '未记录' },
    { label: '编号', value: roll.extraNo || '-' },
    { label: '批次', value: roll.batchNo || '-' },
    { label: '克重', value: formatGram(gram) },
    { label: '门幅', value: formatMm(width) },
    { label: '卷径', value: formatStoredDiameter(roll.originalDiameter) },
    { label: '纸芯', value: formatStoredCoreDiameter(roll.coreDiameter) },
    { label: '总重', value: formatOptionalKg(displayRollWeight(roll)) },
  ]
}

function buildRollDetail(roll: OriginalRoll): string {
  const gram = roll.actualGramWeight ?? roll.gramWeight
  const width = roll.actualWidth ?? roll.originalWidth
  return [
    `批次 ${roll.batchNo || '-'}`,
    `克重 ${formatGram(gram)}`,
    `门幅 ${formatMm(width)}`,
    `卷径 ${formatStoredDiameter(roll.originalDiameter)}`,
    `纸芯 ${formatStoredCoreDiameter(roll.coreDiameter)}`,
    `件数 ${roll.pieceNum ?? 1} 件`,
    `总重 ${formatOptionalKg(displayRollWeight(roll))}`,
  ].join(' · ')
}

function displayRollWeight(roll: OriginalRoll): number | undefined {
  return isRollWeightKnown(roll) ? rollTotalWeight(roll) : undefined
}
