import { describe, expect, it } from 'vitest'
import { mergeBackRecordDisplayValues } from './useBackRecordDisplayValues'

describe('mergeBackRecordDisplayValues', () => {
  it('merges finish output adjustments into the displayed values', () => {
    const merged = mergeBackRecordDisplayValues(
      { finishAdjustments: { first: adjustment('finish-1') } },
      { finishAdjustments: { second: adjustment('finish-2') } },
    )

    expect(Object.keys(merged.finishAdjustments ?? {})).toEqual(['first', 'second'])
  })
})

function adjustment(finishUuid: string) {
  return {
    plannedFinishUuids: [finishUuid],
    producedFinishUuids: [finishUuid],
    reason: '',
    added: [],
  }
}
