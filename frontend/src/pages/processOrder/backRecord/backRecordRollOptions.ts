import type { OriginalRoll } from '../../../types/processOrder'

export function buildBackRecordRollOptions(rolls: OriginalRoll[] = []) {
  return rolls.map((roll, index) => ({
    uuid: roll.uuid,
    rollName: [
      `母卷 ${index + 1}`,
      roll.rollNo || roll.extraNo || '无卷号',
      `${roll.paperName || '-'} / ${roll.gramWeight ?? '-'}g / ${roll.originalWidth ?? '-'}mm`,
    ].join(' | '),
  }))
}
