import type { OriginalRoll } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import type { BackRecordSourceOption } from './BackRecordFinishFields'

export function buildBackRecordSourceOptions(rolls: OriginalRoll[] = []): BackRecordSourceOption[] {
  return rolls
    .map((roll, index) => ({ roll, sequence: index + 1 }))
    .filter(({ roll }) => roll.processMode !== 3)
    .map(({ roll, sequence }) => ({
      label: `母卷 ${sequence} / ${roll.rollNo || roll.extraNo || '未记录卷号'} / ${roll.paperName || '-'}`,
      maxWidth: roll.actualWidth ?? roll.originalWidth,
      processMode: roll.processMode ?? 1,
      value: roll.uuid,
    }))
}

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
