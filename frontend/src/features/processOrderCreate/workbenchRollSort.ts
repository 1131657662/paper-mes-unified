import type { RollDraft } from './types'

export type WorkbenchRollSortMode = 'original' | 'spec' | 'width' | 'weight'
export type WorkbenchRollSortDirection = 'asc' | 'desc'

export interface WorkbenchRollSortPreference {
  mode: WorkbenchRollSortMode
  direction: WorkbenchRollSortDirection
}

export const DEFAULT_WORKBENCH_ROLL_SORT: WorkbenchRollSortPreference = {
  mode: 'original',
  direction: 'asc',
}

const collator = new Intl.Collator('zh-CN', { numeric: true, sensitivity: 'base' })

export function sortWorkbenchRolls(
  rolls: RollDraft[],
  preference: WorkbenchRollSortPreference,
): RollDraft[] {
  const indexed = rolls.map((roll, index) => ({ index, roll }))
  if (preference.mode === 'original') return indexed.map(({ roll }) => roll)

  indexed.sort((left, right) => {
    const compared = compareRolls(left.roll, right.roll, preference)
    return compared || left.index - right.index
  })
  return indexed.map(({ roll }) => roll)
}

function compareRolls(
  left: RollDraft,
  right: RollDraft,
  preference: WorkbenchRollSortPreference,
): number {
  if (preference.mode === 'spec') return compareSpec(left, right)
  const field = preference.mode === 'width' ? 'originalWidth' : 'rollWeight'
  return compareNullableNumber(left[field], right[field], preference.direction)
}

function compareSpec(left: RollDraft, right: RollDraft): number {
  return compareNullableText(left.paperName, right.paperName)
    || compareNullableNumber(left.gramWeight, right.gramWeight, 'asc')
    || compareNullableNumber(left.originalWidth, right.originalWidth, 'asc')
    || compareNullableNumber(left.rollWeight, right.rollWeight, 'asc')
}

function compareNullableText(left: string | undefined, right: string | undefined): number {
  if (!left && !right) return 0
  if (!left) return 1
  if (!right) return -1
  return collator.compare(left, right)
}

function compareNullableNumber(
  left: number | undefined,
  right: number | undefined,
  direction: WorkbenchRollSortDirection,
): number {
  if (left == null && right == null) return 0
  if (left == null) return 1
  if (right == null) return -1
  return (left - right) * (direction === 'asc' ? 1 : -1)
}
