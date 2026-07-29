import type { RollDraft } from './types'

export function mergeImportedRolls(existing: RollDraft[], imported: RollDraft[]): RollDraft[] {
  if (existing.length === 1 && isPlaceholderRoll(existing[0])) return imported
  return [...existing, ...imported]
}

function isPlaceholderRoll(roll?: RollDraft): boolean {
  if (!roll || roll.uuid || Number(roll.rollWeight ?? 0) > 0) return false
  return [
    roll.paperName,
    roll.rollNo,
    roll.extraNo,
    roll.batchNo,
    roll.damageDesc,
    roll.remark,
  ].every((value) => !value?.trim())
}
