import { describe, expect, it } from 'vitest'
import type { BackRecordFinishAdjustmentValues } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import { autoTrimWeights } from './backRecordAutoTrim'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

describe('automatic trim weight calculation', () => {
  it('excludes planned finishes marked as not produced', () => {
    const item: BackRecordWorkItem = {
      key: 'roll-roll-1',
      kind: 'roll',
      title: 'Roll 1',
      roll: { uuid: 'roll-1', actualWeight: 400, processMode: 1 },
      rollProductions: [],
      isMergeGroup: false,
      sourceMode: 'linked',
      finishes: [
        ...['finish-1', 'finish-2', 'finish-3', 'finish-4'].map((uuid) => ({
          bindMode: 'linked' as const,
          finish: { uuid, isSpare: 0, isRemain: 0 },
        })),
        { bindMode: 'linked', finish: { uuid: 'trim-1', isSpare: 0, isRemain: 1 } },
      ],
    }
    const adjustment: BackRecordFinishAdjustmentValues = {
      plannedFinishUuids: ['finish-1', 'finish-2', 'finish-3', 'finish-4'],
      producedFinishUuids: ['finish-1', 'finish-2'],
      reason: 'actual output was lower than planned',
      added: [],
    }
    const values: BackRecordFormValues = {
      rolls: { 'roll-1': { actualWeight: 400 } },
      finishes: {
        'finish-1': { actualWeight: 90 },
        'finish-2': { actualWeight: 90 },
      },
    }

    const patches = autoTrimWeights(item, values, {
      autoTrimUuids: new Set(),
      manualTrimUuids: new Set(),
      adjustment,
    })

    expect(patches).toEqual([{ uuid: 'trim-1', actualWeight: 220 }])
  })
})
