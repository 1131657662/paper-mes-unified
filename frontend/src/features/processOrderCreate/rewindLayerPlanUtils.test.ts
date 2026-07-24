import { describe, expect, it } from 'vitest'
import { sameSpecRewindPlan } from './rewindLayerPlanUtils'
import type { RollDraft } from './types'

describe('sameSpecRewindPlan', () => {
  it('creates exactly one unchanged finish from the source roll', () => {
    const roll = {
      localId: 'local-1',
      uuid: 'roll-1',
      paperName: '牛卡纸',
      gramWeight: 265,
      originalWidth: 1702,
      originalDiameter: 48,
      coreDiameter: 6,
      rollWeight: 850,
      pieceNum: 1,
      processMode: 1,
      mainStepType: 2,
    } satisfies RollDraft

    const result = sameSpecRewindPlan({ processMode: 1, rewindMode: 3, segments: [] }, roll)

    expect(result.rewindMode).toBe(6)
    expect(result.segments).toEqual([{
      segmentSort: 1,
      segmentRatio: 1,
      targetDiameter: 48,
      finishCoreDiameter: 6,
      repeatCount: 1,
      sources: [{ originalUuid: 'roll-1', shareRatio: 100, consumeRatio: 100, sourceSort: 1 }],
      layoutItems: [{ width: 1702, quantity: 1, itemType: 'FINISH' }],
    }])
  })
})
