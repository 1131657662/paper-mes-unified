import { describe, expect, it } from 'vitest'
import { formatFixedNumberInput, formatWholeKg } from './numberFormatters'

describe('重量小数格式化', () => {
  it('客户重量输入框按整公斤显示', () => {
    expect(formatFixedNumberInput(1171.1)).toBe('1171')
  })

  it('客户重量汇总按整公斤四舍五入', () => {
    expect(formatWholeKg(9361.5)).toBe('9362 kg')
  })
})
