import type { RollDraft } from './types'

export type EntryWeightStatus = 'UNKNOWN' | 'ESTIMATED'

export function updateRollWeightStatus(
  rolls: RollDraft[],
  localId: string,
  weightStatus: EntryWeightStatus,
): RollDraft[] {
  return rolls.map((roll) => roll.localId === localId
    ? { ...roll, weightStatus, rollWeight: weightStatus === 'UNKNOWN' ? undefined : roll.rollWeight }
    : roll)
}
