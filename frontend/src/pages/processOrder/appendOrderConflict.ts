import { rollDraftFromOriginal } from '../../features/processOrderCreate/draftMappers'
import type { RollDraft } from '../../features/processOrderCreate/types'
import type { ProcessOrderAppendSessionVO } from '../../types/processOrder'

export interface AppendConflictSummary {
  added: number
  changed: number
  removed: number
}

export function summarizeAppendConflict(
  previous: ProcessOrderAppendSessionVO,
  latest: ProcessOrderAppendSessionVO,
): AppendConflictSummary {
  const previousById = rollMap(previous)
  const latestById = rollMap(latest)
  let changed = 0
  for (const [uuid, roll] of latestById) {
    const oldRoll = previousById.get(uuid)
    if (oldRoll && JSON.stringify(oldRoll) !== JSON.stringify(roll)) changed++
  }
  return {
    added: Array.from(latestById.keys()).filter((uuid) => !previousById.has(uuid)).length,
    changed,
    removed: Array.from(previousById.keys()).filter((uuid) => !latestById.has(uuid)).length,
  }
}

export function mergeAppendConflictRolls(
  previous: ProcessOrderAppendSessionVO,
  local: RollDraft[],
  latest: ProcessOrderAppendSessionVO,
): RollDraft[] {
  const previousIds = new Set((previous.rolls ?? []).map((roll) => roll.uuid))
  const latestById = rollMap(latest)
  const localIds = new Set(local.map((roll) => roll.uuid).filter(Boolean))
  const recoveredLocal = local.map((roll) => {
    if (!roll.uuid || latestById.has(roll.uuid)) return roll
    return { ...roll, uuid: undefined }
  })
  const remoteAdditions = (latest.rolls ?? [])
    .filter((roll) => roll.uuid && !previousIds.has(roll.uuid) && !localIds.has(roll.uuid))
    .map(rollDraftFromOriginal)
  return [...recoveredLocal, ...remoteAdditions]
}

function rollMap(session: ProcessOrderAppendSessionVO) {
  return new Map((session.rolls ?? [])
    .filter((roll) => Boolean(roll.uuid))
    .map((roll) => [roll.uuid, roll] as const))
}
