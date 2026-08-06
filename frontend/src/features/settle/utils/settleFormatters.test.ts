import { describe, expect, it } from 'vitest'
import { formatPercent } from './settleFormatters'

describe('结算收款进度格式化', () => {
  it('保留低于百分之一的非零进度', () => {
    expect(formatPercent(1, 2274)).toBe('0.04%')
  })

  it('无应收金额时显示零进度', () => {
    expect(formatPercent(0, 0)).toBe('0%')
  })
})
