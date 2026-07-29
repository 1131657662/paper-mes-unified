import { describe, expect, it } from 'vitest'
import { isRollReadyForSave, previewsFromBatch } from './createOrderState'
import type { RollDraft } from './types'

describe('batch preview mapping', () => {
  it('maps previews by original uuid instead of response order', () => {
    const first = roll('local-1', 'roll-1')
    const second = roll('local-2', 'roll-2')

    const result = previewsFromBatch([first, second], [
      { originalUuid: 'roll-2', ready: false },
      { originalUuid: 'roll-1', ready: true },
    ])

    expect(result).toEqual({
      'local-1': { originalUuid: 'roll-1', ready: true },
      'local-2': { originalUuid: 'roll-2', ready: false },
    })
  })
})

describe('isRollReadyForSave', () => {
  it('rejects a required numeric field that was cleared', () => {
    expect(isRollReadyForSave({ ...roll('local-1', 'roll-1'), originalWidth: 0 })).toBe(false)
  })

  it('requires a physical roll number for direct shipment', () => {
    expect(isRollReadyForSave({ ...roll('local-1', 'roll-1'), processMode: 3, rollNo: '' })).toBe(false)
  })
})

function roll(localId: string, uuid: string): RollDraft {
  return {
    localId,
    uuid,
    paperName: 'test paper',
    gramWeight: 80,
    originalWidth: 1000,
    rollWeight: 500,
    processMode: 1,
    mainStepType: 2,
  }
}
