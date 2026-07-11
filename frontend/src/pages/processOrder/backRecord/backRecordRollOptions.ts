import type { OriginalRoll } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'

export function buildBackRecordRollOptions(rolls: OriginalRoll[] = []) {
  return rolls.map((roll, index) => ({
    uuid: roll.uuid,
    rollName: [
      `母卷 ${index + 1}`,
      roll.rollNo || roll.extraNo || '无卷号',
      `${roll.paperName || '-'} / ${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)}`,
    ].join(' | '),
  }))
}
