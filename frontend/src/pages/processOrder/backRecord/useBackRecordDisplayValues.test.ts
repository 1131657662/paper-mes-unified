import { describe, expect, it } from 'vitest'
import type { BackRecordFormValues } from './backRecordUtils'
import { mergeBackRecordDisplayValues } from './useBackRecordDisplayValues'

describe('back-record display values', () => {
  it('keeps edited values when a work item is unmounted', () => {
    const fallback: BackRecordFormValues = {
      finishes: { 'finish-1': { actualWeight: 10 } },
      rolls: { 'roll-1': { actualWeight: 100 } },
    }
    const watched: BackRecordFormValues = {
      finishes: { 'finish-1': { actualWeight: 95 } },
      rolls: { 'roll-1': { actualWeight: 100 } },
    }

    const values = mergeBackRecordDisplayValues(fallback, watched)

    expect(values.finishes?.['finish-1']?.actualWeight).toBe(95)
  })
})
