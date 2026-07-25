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
      .filter((roll) => roll.processMode === 4 && roll.uuid)
      .map((roll) => roll.localId)
  }
  return options.rolls
    .filter((roll) => matchesSelectedSpec(roll, options.selected))
    .filter((roll) => !options.locks[roll.localId])
    .map((roll) => roll.localId)
}

export function planBatchTargets(options: BatchTargetOptions): RollDraft[] {
  if (!options.selected || !supportsSinglePlanEditing(options.selected.processMode)) return []
  return options.rolls.filter((roll) => Boolean(
    options.checkedIds.includes(roll.localId)
    && !options.locks[roll.localId]
    && roll.processMode === options.selected?.processMode
    && roll.mainStepType === options.selected?.mainStepType
    && roll.uuid
    && !options.routePreviews[roll.uuid],
  ))
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
