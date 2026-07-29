import type { ProcessRoutePreviewVO } from '../../types/processOrder'
import type { MergedSourceLock } from './rewindConsumptionUtils'
import type { RollDraft } from './types'

type RollLocks = Record<string, MergedSourceLock>

export function configurableRolls(rolls: RollDraft[], locks: RollLocks): RollDraft[] {
  return rolls.filter((roll) => roll.processMode !== 3 && !locks[roll.localId])
}

export function supportsSinglePlanEditing(processMode?: number): boolean {
  return processMode === 1 || processMode === 2
}

export function supportsRouteDesigner(processMode?: number): boolean {
  return processMode === 1
}

export function selectedConfigRoll(
  rolls: RollDraft[],
  selectedId: string | undefined,
  locks: RollLocks,
): RollDraft | undefined {
  const candidates = configurableRolls(rolls, locks)
  return candidates.find((roll) => roll.localId === selectedId) ?? candidates[0]
}

export function sameSpecRollIds(options: SameSpecOptions): string[] {
  if (options.selected.processMode === 4) {
    return configurableRolls(options.rolls, options.locks)
      .filter((roll) => roll.processMode === 4 && roll.uuid && roll.localId !== options.selected.localId)
      .map((roll) => roll.localId)
  }
  return options.rolls
    .filter((roll) => matchesSelectedSpec(roll, options.selected))
    .filter((roll) => roll.localId !== options.selected.localId)
    .filter((roll) => !options.locks[roll.localId])
    .map((roll) => roll.localId)
}

export function planBatchTargets(options: BatchTargetOptions): RollDraft[] {
  const selected = options.selected
  if (!selected || !supportsSinglePlanEditing(selected.processMode)) return []
  return options.rolls.filter((roll) => Boolean(
    options.checkedIds.includes(roll.localId)
    && roll.localId !== selected.localId
    && !options.locks[roll.localId]
    && matchesSelectedSpec(roll, selected)
    && roll.uuid
    && !options.routePreviews[roll.uuid],
  ))
}

export function planBatchSelectionReasons(options: SelectionReasonOptions): Record<string, string> {
  return Object.fromEntries(options.rolls.flatMap((roll) => {
    const reason = planSelectionReason(roll, options)
    return reason ? [[roll.localId, reason]] : []
  }))
}

export function serviceBatchSelectionReasons(options: SelectionReasonOptions): Record<string, string> {
  return Object.fromEntries(options.rolls.flatMap((roll) => {
    const reason = serviceSelectionReason(roll, options)
    return reason ? [[roll.localId, reason]] : []
  }))
}

function planSelectionReason(roll: RollDraft, options: SelectionReasonOptions) {
  const selected = options.selected
  if (!selected) return '请先选择当前母卷'
  if (roll.localId === selected.localId) return '当前母卷无需加入批量范围'
  const lock = options.locks[roll.localId]
  if (lock) return `已被 ${lock.ownerLabel ?? '其他工艺'} 合并使用`
  if (!roll.uuid) return '请先保存该母卷明细'
  if (!supportsSinglePlanEditing(selected.processMode)) return '当前加工方式不支持批量应用主工艺'
  if (roll.uuid && options.routePreviews[roll.uuid]) return '已配置链式工艺，不能覆盖'
  if (!matchesSelectedSpec(roll, selected)) return '加工方式、主工艺或原纸规格与当前母卷不同'
  return undefined
}

function serviceSelectionReason(roll: RollDraft, options: SelectionReasonOptions) {
  const selected = options.selected
  if (!selected) return '请先选择当前母卷'
  if (roll.localId === selected.localId) return '当前母卷无需加入批量范围'
  const lock = options.locks[roll.localId]
  if (lock) return `已被 ${lock.ownerLabel ?? '其他工艺'} 合并使用`
  if (!roll.uuid) return '请先保存该母卷明细'
  if (roll.processMode === 3) return '直发卷不配置附加工艺'
  if (selected.processMode === 4 && roll.processMode !== 4) return '仅附加工艺只能批量应用到同类母卷'
  return undefined
}

function matchesSelectedSpec(roll: RollDraft, selected: RollDraft) {
  return roll.processMode === selected.processMode
    && roll.mainStepType === selected.mainStepType
    && roll.paperName === selected.paperName
    && roll.gramWeight === selected.gramWeight
    && roll.originalWidth === selected.originalWidth
}

interface SameSpecOptions {
  locks: RollLocks
  rolls: RollDraft[]
  selected: RollDraft
}

interface BatchTargetOptions {
  checkedIds: string[]
  locks: RollLocks
  rolls: RollDraft[]
  routePreviews: Record<string, ProcessRoutePreviewVO>
  selected?: RollDraft
}

interface SelectionReasonOptions {
  locks: RollLocks
  rolls: RollDraft[]
  routePreviews: Record<string, ProcessRoutePreviewVO>
  selected?: RollDraft
}
