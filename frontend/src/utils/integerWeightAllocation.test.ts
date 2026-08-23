import { describe, expect, it } from 'vitest'
import { allocateIntegerWeight, roundWeightTotal } from './integerWeightAllocation'

describe('integerWeightAllocation', () => {
  it('ignores non-finite bases instead of producing NaN', () => {
    expect(allocateIntegerWeight(10, [Infinity, 2])).toEqual([0, 10])
    expect(allocateIntegerWeight(10, [Number.NaN, 2])).toEqual([0, 10])
  })

  it('normalizes non-finite totals to a safe zero for display helpers', () => {
    expect(roundWeightTotal(Infinity)).toBe(0)
    expect(roundWeightTotal(Number.NaN)).toBe(0)
  })
})
