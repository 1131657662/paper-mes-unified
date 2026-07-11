import type { RewindSourcePlanDTO } from '../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../utils/numberFormatters'
import type { RollDraft } from './types'

export interface SourceRollOption {
  value: string
  label: string
  weight: number
  roll: RollDraft
}

export function sourceOptionsFromRolls(rolls: RollDraft[]): SourceRollOption[] {
  return rolls
    .filter((roll) => roll.uuid)
    .map((roll, index) => ({
      value: roll.uuid!,
      label: sourceRollLabel(roll, index),
      weight: rollTotalWeight(roll),
      roll,
    }))
}

export function rollTotalWeight(roll: RollDraft): number {
  return Number(roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
}

export function equalSources(values: string[]): RewindSourcePlanDTO[] {
  const share = values.length ? roundRatio(100 / values.length) : 0
  return normalizeRatio(values.map((value, index) => ({ originalUuid: value, shareRatio: share, sourceSort: index + 1 })))
}

export function weightSources(values: string[], options: SourceRollOption[]): RewindSourcePlanDTO[] {
  const weights = new Map(options.map((option) => [option.value, option.weight]))
  const total = values.reduce((sum, value) => sum + (weights.get(value) ?? 0), 0)
  if (total <= 0) return equalSources(values)
  return normalizeRatio(values.map((value, index) => ({
    originalUuid: value,
    shareRatio: roundRatio(((weights.get(value) ?? 0) / total) * 100),
    sourceSort: index + 1,
  })))
}

export function sameSpecSourceIds(target: RollDraft, rolls: RollDraft[]): string[] {
  return rolls
    .filter((roll) => roll.uuid)
    .filter((roll) => roll.paperName === target.paperName)
    .filter((roll) => roll.gramWeight === target.gramWeight)
    .filter((roll) => roll.originalWidth === target.originalWidth)
    .map((roll) => roll.uuid!)
}

export function sourceTotalWeight(sources: RewindSourcePlanDTO[], options: SourceRollOption[]): number {
  const weights = new Map(options.map((option) => [option.value, option.weight]))
  return sources.reduce((sum, source) => sum + (source.originalUuid ? weights.get(source.originalUuid) ?? 0 : 0), 0)
}

export function sourceRatioTotal(sources: RewindSourcePlanDTO[]): number {
  return roundRatio(sources.reduce((sum, source) => sum + Number(source.shareRatio ?? 0), 0))
}

export function patchSource(
  sources: RewindSourcePlanDTO[],
  index: number,
  patch: Partial<RewindSourcePlanDTO>,
): RewindSourcePlanDTO[] {
  return sources.map((source, itemIndex) => (itemIndex === index ? { ...source, ...patch } : source))
}

export function labelForSource(source: RewindSourcePlanDTO, options: SourceRollOption[]): string {
  return options.find((option) => option.value === source.originalUuid)?.label ?? source.originalUuid ?? '未选择'
}

function normalizeRatio(sources: RewindSourcePlanDTO[]): RewindSourcePlanDTO[] {
  if (!sources.length) return sources
  const totalExceptLast = sources.slice(0, -1).reduce((sum, source) => sum + Number(source.shareRatio ?? 0), 0)
  return sources.map((source, index) => (
    index === sources.length - 1 ? { ...source, shareRatio: roundRatio(100 - totalExceptLast) } : source
  ))
}

function roundRatio(value: number): number {
  return Number(value.toFixed(2))
}

function sourceRollLabel(roll: RollDraft, index: number): string {
  const identity = roll.rollNo || roll.extraNo || '无卷号'
  const spec = `${roll.paperName || '-'} / ${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)}`
  const weight = formatKg(rollTotalWeight(roll))
  return `母卷${index + 1}｜${identity}｜${spec}｜${weight}`
}
