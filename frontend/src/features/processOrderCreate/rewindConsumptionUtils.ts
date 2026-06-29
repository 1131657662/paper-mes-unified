import type { ProcessPlanDTO, RewindSegmentPlanDTO, RewindSourcePlanDTO } from '../../types/processOrder'
import type { RollDraft } from './types'
import type { SourceRollOption } from './rewindSourceUtils'

export interface SourceUsageRow {
  originalUuid: string
  label: string
  consumeRatio: number
  remainingRatio: number
  consumeWeight: number
  status: 'ok' | 'warning' | 'error'
}

export interface MergedSourceLock {
  ownerLocalId: string
  ownerLabel: string
  consumeRatio: number
}

export function sourceConsumptionValue(source: RewindSourcePlanDTO): number {
  return Number(source.consumeRatio ?? source.shareRatio ?? 0)
}

export function consumptionSources(values: string[], current: RewindSourcePlanDTO[] = []): RewindSourcePlanDTO[] {
  return values.map((value, index) => {
    const existing = current.find((source) => source.originalUuid === value)
    return {
      ...existing,
      originalUuid: value,
      consumeRatio: existing ? sourceConsumptionValue(existing) : 100,
      sourceSort: index + 1,
    }
  })
}

export function fullConsumptionSources(values: string[]): RewindSourcePlanDTO[] {
  return values.map((value, index) => ({ originalUuid: value, consumeRatio: 100, sourceSort: index + 1 }))
}

export function equalConsumptionSources(values: string[]): RewindSourcePlanDTO[] {
  const ratio = values.length ? roundRatio(100 / values.length) : 0
  return values.map((value, index) => ({ originalUuid: value, consumeRatio: ratio, sourceSort: index + 1 }))
}

export function segmentConsumedWeight(sources: RewindSourcePlanDTO[], options: SourceRollOption[]): number {
  const weights = weightMap(options)
  return sources.reduce((sum, source) => {
    const sourceWeight = source.originalUuid ? weights.get(source.originalUuid) ?? 0 : 0
    return sum + sourceWeight * sourceConsumptionValue(source) / 100
  }, 0)
}

export function sourceCompositionRatio(
  source: RewindSourcePlanDTO,
  sources: RewindSourcePlanDTO[],
  options: SourceRollOption[],
): number {
  const total = segmentConsumedWeight(sources, options)
  if (!source.originalUuid || total <= 0) return 0
  const sourceWeight = weightMap(options).get(source.originalUuid) ?? 0
  return roundRatio(sourceWeight * sourceConsumptionValue(source) / total)
}

export function sourceUsageRows(segments: RewindSegmentPlanDTO[], options: SourceRollOption[]): SourceUsageRow[] {
  const totals = new Map<string, number>()
  for (const source of segments.flatMap((segment) => segment.sources ?? [])) {
    if (!source.originalUuid) continue
    totals.set(source.originalUuid, roundRatio((totals.get(source.originalUuid) ?? 0) + sourceConsumptionValue(source)))
  }
  return Array.from(totals.entries()).map(([originalUuid, consumeRatio]) => {
    const option = options.find((item) => item.value === originalUuid)
    const remainingRatio = roundRatio(100 - consumeRatio)
    return {
      originalUuid,
      label: option?.label ?? originalUuid,
      consumeRatio,
      remainingRatio,
      consumeWeight: (option?.weight ?? 0) * consumeRatio / 100,
      status: usageStatus(consumeRatio),
    }
  })
}

export function mergedSourceLocks(
  rolls: RollDraft[],
  plans: Record<string, ProcessPlanDTO>,
): Record<string, MergedSourceLock> {
  const rollByLocalId = new Map(rolls.map((roll) => [roll.localId, roll]))
  const rollByUuid = new Map(rolls.filter((roll) => roll.uuid).map((roll) => [roll.uuid!, roll]))
  const locks: Record<string, MergedSourceLock> = {}

  for (const [ownerLocalId, plan] of Object.entries(plans)) {
    const owner = rollByLocalId.get(ownerLocalId)
    if (!owner?.uuid || !isMultiSourcePlan(plan)) continue
    addLocksForPlan({ plan, owner, rollByUuid, locks, ownerLocalId })
  }
  return locks
}

export function mergedSourceUuidSet(rolls: RollDraft[], plans: Record<string, ProcessPlanDTO>): Set<string> {
  const locks = mergedSourceLocks(rolls, plans)
  return new Set(rolls.filter((roll) => roll.uuid && locks[roll.localId]).map((roll) => roll.uuid!))
}

function addLocksForPlan(options: AddLockOptions): void {
  const ownerLabel = rollShortLabel(options.owner)
  for (const source of options.plan.segments?.flatMap((segment) => segment.sources ?? []) ?? []) {
    if (!source.originalUuid || source.originalUuid === options.owner.uuid) continue
    const lockedRoll = options.rollByUuid.get(source.originalUuid)
    if (!lockedRoll) continue
    const previous = options.locks[lockedRoll.localId]
    options.locks[lockedRoll.localId] = {
      ownerLocalId: options.ownerLocalId,
      ownerLabel,
      consumeRatio: roundRatio((previous?.consumeRatio ?? 0) + sourceConsumptionValue(source)),
    }
  }
}

function isMultiSourcePlan(plan: ProcessPlanDTO): boolean {
  return plan.processMode !== 3 && plan.mainStepType === 2 && plan.rewindMode === 5
}

function usageStatus(value: number): SourceUsageRow['status'] {
  if (value > 100.01) return 'error'
  if (Math.abs(value - 100) <= 0.01) return 'ok'
  return 'warning'
}

function weightMap(options: SourceRollOption[]): Map<string, number> {
  return new Map(options.map((option) => [option.value, option.weight]))
}

function rollShortLabel(roll: RollDraft): string {
  return roll.rollNo || roll.extraNo || roll.paperName || '未命名母卷'
}

function roundRatio(value: number): number {
  return Number(value.toFixed(2))
}

interface AddLockOptions {
  plan: ProcessPlanDTO
  owner: RollDraft
  ownerLocalId: string
  rollByUuid: Map<string, RollDraft>
  locks: Record<string, MergedSourceLock>
}
