import { describe, expect, it } from 'vitest'
import { isRollWeightKnown, rollTotalWeight } from './routeConfigSource'

describe('routeConfigSource', () => {
  it('uses an integer source budget for fractional actual weights', () => {
    const roll = { actualWeight: 1000.4, pieceNum: 1 }

    expect(rollTotalWeight(roll)).toBe(1000)
    expect(isRollWeightKnown(roll)).toBe(true)
  })

  it('does not treat Infinity as a known source weight', () => {
    const roll = { rollWeight: Infinity, pieceNum: 1 }

    expect(rollTotalWeight(roll)).toBe(0)
    expect(isRollWeightKnown(roll)).toBe(false)
  })
})
