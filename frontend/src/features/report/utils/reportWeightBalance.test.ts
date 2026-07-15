import { describe, expect, it } from 'vitest'
import { hasWeightGain, weightGain } from './reportWeightBalance'

describe('reportWeightBalance', () => {
  it('只在成品重量实质高于原纸重量时报警', () => {
    expect(hasWeightGain(1000, 1000.001)).toBe(false)
    expect(hasWeightGain(1000, 1000.01)).toBe(true)
  })

  it('缺失重量按零处理', () => {
    expect(weightGain(undefined, 12)).toBe(12)
  })
})
