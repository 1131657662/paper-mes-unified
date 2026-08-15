import { describe, expect, it } from 'vitest'
import type { OriginalRoll } from '../../types/processOrder'
import { rollDraftFromOriginal, toOriginalRoll, toRollDto } from './draftMappers'

describe('draft roll actual width mapping', () => {
  const original: OriginalRoll = {
    uuid: 'roll-1',
    paperName: '牛卡纸',
    gramWeight: 265,
    originalWidth: 1702,
    actualWidth: 1680,
    originalDiameter: 48,
    coreDiameter: 6,
  }

  it('keeps actualWidth when hydrating and writing a draft roll', () => {
    const draft = rollDraftFromOriginal(original)

    expect(toRollDto(draft).actualWidth).toBe(1680)
    expect(toOriginalRoll(draft).actualWidth).toBe(1680)
  })
})
